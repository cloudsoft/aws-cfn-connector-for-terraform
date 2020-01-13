package io.cloudsoft.terraform.infrastructure;

import com.google.common.base.Preconditions;
import io.cloudsoft.terraform.infrastructure.commands.RemoteSystemdUnit;
import io.cloudsoft.terraform.infrastructure.commands.TerraformSshCommands;
import lombok.Getter;
import software.amazon.cloudformation.proxy.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class TerraformBaseWorker<Steps extends Enum<?>> {

    // Mirror Terraform, which maxes its state checks at 10 seconds when working on long jobs
    private static final int MAX_CHECK_INTERVAL_SECONDS = 10;
    // Use YAML doc separator to separate logged messages
    public static final CharSequence LOG_MESSAGE_SEPARATOR = "---";

    @Getter
    protected AmazonWebServicesClientProxy proxy;
    @Getter
    protected ResourceHandlerRequest<ResourceModel> request;
    @Getter
    protected ResourceModel model;
    @Getter
    protected CallbackContext callbackContext;
    @Getter
    private Logger logger;

    protected TerraformParameters parameters;

    protected Steps currentStep;

    // === init and accessors ========================

    protected void init(
            @Nullable AmazonWebServicesClientProxy proxy,
            ResourceHandlerRequest<ResourceModel> request,
            @Nullable CallbackContext callbackContext,
            Logger logger) {
        if (this.request!=null) {
            throw new IllegalStateException("Handler can only be setup and used once, and request has already been initialized when attempting to re-initialize it");
        }
        this.proxy = proxy;
        this.request = Preconditions.checkNotNull(request, "request");
        this.model = request.getDesiredResourceState();
        this.callbackContext = callbackContext!=null ? callbackContext : new CallbackContext();
        this.logger = Preconditions.checkNotNull(logger, "logger");
    }

    public synchronized TerraformParameters getParameters() {
        if (parameters==null) {
            if (proxy==null) {
                throw new IllegalStateException("Parameters cannot be accessed before proxy set during init");
            }
            parameters = new TerraformParameters(proxy);
        }
        return parameters;
    }

    // for testing
    public synchronized void setParameters(TerraformParameters parameters) {
        if (this.parameters!=null) {
            throw new IllegalStateException("Handler can only be setup and used once, and parameters have already initialized when attempting to re-initializee them");
        }
        this.parameters = Preconditions.checkNotNull(parameters, "parameters");
    }

    // === lifecycle ========================

    public final ProgressEvent<ResourceModel, CallbackContext> runHandlingError() {
        try {
            logger.log(getClass().getName() + " lambda starting, model: "+model+", callback: "+callbackContext);
            ProgressEvent<ResourceModel, CallbackContext> result = runStep();
            logger.log(getClass().getName() + " lambda exiting, status: "+result.getStatus()+", callback: "+result.getCallbackContext()+", message: "+result.getMessage());
            return result;

        } catch (ConnectorHandlerFailures.Handled e) {
            logger.log(getClass().getName() + " lambda exiting with error");
            return statusFailed("FAILING: "+e.getMessage());

        } catch (ConnectorHandlerFailures.Unhandled e) {
            if (e.getCause()!=null) {
                logException("FAILING: "+e.getMessage(), e.getCause());
            } else {
                log("FAILING: "+e.getMessage());
            }
            logger.log(getClass().getName() + " lambda exiting with error");
            return statusFailed(e.getMessage());

        } catch (Exception e) {
            logException("FAILING: "+e, e);
            logger.log(getClass().getName() + " lambda exiting with error");
            return statusFailed((currentStep!=null ? currentStep+": " : "")+e);
        }
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException;

    // === utils ========================

    protected void log(String message) {
        System.out.println(message);
        System.out.println(LOG_MESSAGE_SEPARATOR);
        if (logger!=null) {
            logger.log(message);
        }
    }

    protected final void logException(String message, Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        log(message + "\n" + sw.toString());
    }


    protected ProgressEvent<ResourceModel, CallbackContext> statusFailed(String message) {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.FAILED)
                .message(message)
                .build();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> statusSuccess() {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
    }

    protected ProgressEvent<ResourceModel, CallbackContext> statusInProgress() {
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .callbackContext(callbackContext)
                .callbackDelaySeconds(nextDelay(callbackContext))
                .status(OperationStatus.IN_PROGRESS)
                .message(currentStep == null ? null : "Step: " + currentStep)
                .build();
    }

    int nextDelay(CallbackContext callbackContext) {
        if (callbackContext.lastDelaySeconds < 0) {
            callbackContext.lastDelaySeconds = 0;
        } else if (callbackContext.lastDelaySeconds == 0) {
            callbackContext.lastDelaySeconds = 1;
        } else if (callbackContext.lastDelaySeconds < MAX_CHECK_INTERVAL_SECONDS) {
            // exponential backoff
            callbackContext.lastDelaySeconds =
                    Math.min(MAX_CHECK_INTERVAL_SECONDS, 2 * callbackContext.lastDelaySeconds);
        }
        return callbackContext.lastDelaySeconds;
    }

    protected final void advanceTo(Steps nextStep) {
        logger.log("Entering step "+nextStep);
        callbackContext.stepId = nextStep.toString();
        callbackContext.lastDelaySeconds = -1;
    }

    protected final TerraformSshCommands tfSshCommands() {
        return TerraformSshCommands.of(this);
    }

    protected RemoteSystemdUnit tfInit() {
        return RemoteSystemdUnit.of(this, "terraform-init");
    }

    protected RemoteSystemdUnit tfApply() {
        return RemoteSystemdUnit.of(this, "terraform-apply");
    }

    protected RemoteSystemdUnit tfDestroy() {
        return RemoteSystemdUnit.of(this, "terraform-destroy");
    }

    protected boolean checkStillRunningOrError(RemoteSystemdUnit process) throws IOException {
        if (process.isRunning()) {
            return true;
        }
        if (process.wasFailure()) {
            String message = "Error in " + process.getUnitName()+": result "+process.getResult()+" ("+
                process.getMainExitCode()+")";
            logger.log(message+"\n"+process.getLog());
            throw ConnectorHandlerFailures.handled(message+"; see CloudWatch logs for more detail.");
        }
        return false;
    }

    // This call actually consists of two network transfers, hence for large files is more
    // likely to time out. However, splitting it into two FSM states would require some place
    // to keep the downloaded file. The callback context isn't intended for that, neither is
    // the lambda's runtime filesystem.
    // There would be one more transfer if the CloudFormation template defines any Terraform
    // variables, so the above note would apply even more.
    protected final void getAndUploadConfiguration() throws IOException {
        tfSshCommands().uploadConfiguration(getParameters().getConfiguration(model), model.getVariables());
    }

}

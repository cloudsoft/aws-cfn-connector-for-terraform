package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import io.cloudsoft.terraform.infrastructure.commands.RemoteSystemdUnit;
import io.cloudsoft.terraform.infrastructure.commands.TerraformSshCommands;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public abstract class TerraformBaseHandler<Steps extends Enum<?>> extends BaseHandler<CallbackContext> {

    // Mirror Terraform, which maxes its state checks at 10 seconds when working on long jobs
    private static final int MAX_CHECK_INTERVAL_SECONDS = 10;
    // Use YAML doc separator to separate logged messages 
    public static final CharSequence LOG_MESSAGE_SEPARATOR = "---";

    // TODO accessors or other tidy, rather than public
    public AmazonWebServicesClientProxy proxy;
    public ResourceHandlerRequest<ResourceModel> request;
    public ResourceModel model;
    public CallbackContext callbackContext;
    public Logger logger;
    
    TerraformParameters parameters;
    
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
            parameters = new TerraformParameters();
        }
        return parameters;
    }
    public synchronized void setParameters(TerraformParameters parameters) {
        if (this.parameters!=null) {
            throw new IllegalStateException("Handler can only be setup and used once, and parameters have already initialized when attempting to re-initializee them");
        }
        this.parameters = Preconditions.checkNotNull(parameters, "parameters");
    }
    
    
    // === lifecycle ========================
    
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        
        init(proxy, request, callbackContext, logger);
        return runWithLoopingIfNecessary();
    }
    
    // TODO: This is now obsolete as the cfn-cli handles reinvokes out of the box. Should remove it.
    protected ProgressEvent<ResourceModel, CallbackContext> runWithLoopingIfNecessary() {
        // allows us to force synchronous behaviour -- especially useful when running in SAM
        boolean forceSynchronous = callbackContext == null ? false : callbackContext.forceSynchronous;
        boolean disregardCallbackDelay = callbackContext == null ? false : callbackContext.disregardCallbackDelay;

        while (true) {
            ProgressEvent<ResourceModel, CallbackContext> result = runHandlingError();
            if (!forceSynchronous || !OperationStatus.IN_PROGRESS.equals(result.getStatus())) {
                return result;
            }
            log("Synchronous mode: " + result.getCallbackContext());
            try {
                if (disregardCallbackDelay) {
                    log("Will run callback immediately");
                } else {
                    log("Will run callback after " + result.getCallbackDelaySeconds() + " seconds");
                    Thread.sleep(1000 * result.getCallbackDelaySeconds());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Aborted due to interrupt", e);
            }
            callbackContext = result.getCallbackContext();
        }
    }
    
    public final ProgressEvent<ResourceModel, CallbackContext> runHandlingError() {
        logger.log(getClass().getName() + " lambda starting: " + model);

        try {
            ProgressEvent<ResourceModel, CallbackContext> result = runStep();
            logger.log(getClass().getName() + " lambda exiting, status: "+result.getStatus()+", callback: " + callbackContext);
            return result;
            
        } catch (Exception e) {
            logException(getClass().getName(), e);
            logger.log(getClass().getName() + " lambda exiting with error");
            return progressEvents().failed();
        }
    }
    
    protected abstract ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException;

    // === utils ========================
    
    protected void log(String message) {
        System.out.println(message);
        System.out.println(LOG_MESSAGE_SEPARATOR);
        logger.log(message);
    }

    protected final void logException(String origin, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        log(origin + " error: " + e + "\n" + sw.toString());
    }


    protected ProgressEvents progressEvents() {
        return new ProgressEvents();
    }
    
    protected class ProgressEvents {
            
        protected ProgressEvent<ResourceModel, CallbackContext> failed() {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.FAILED)
                .build();
        }
        
        protected ProgressEvent<ResourceModel, CallbackContext> success() {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();
        }
        
        protected ProgressEvent<ResourceModel, CallbackContext> inProgressResult() {
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .callbackContext(callbackContext)
                .callbackDelaySeconds(nextDelay(callbackContext))
                .status(OperationStatus.IN_PROGRESS)
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
    }

    protected final void advanceTo(Steps nextStep) {
        logger.log(String.format("advanceTo(): %s -> %s", callbackContext.stepId, nextStep));
        callbackContext.stepId = nextStep.toString();
        callbackContext.lastDelaySeconds = -1;
    }

    // TODO command
    protected final TerraformSshCommands tfSync() {
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


    // This call actually consists of two network transfers, hence for large files is more
    // likely to time out. However, splitting it into two FSM states would require some place
    // to keep the downloaded file. The callback context isn't intended for that, neither is
    // the lambda's runtime filesystem.
    protected final void getAndUploadConfiguration() throws IOException {
        tfSync().uploadConfiguration(getParameters().getConfiguration(proxy, model));
    }

}

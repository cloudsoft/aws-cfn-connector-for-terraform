package io.cloudsoft.terraform.infrastructure.worker;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import io.cloudsoft.terraform.infrastructure.CallbackContext;
import io.cloudsoft.terraform.infrastructure.ResourceModel;
import io.cloudsoft.terraform.infrastructure.TerraformBaseHandler;
import io.cloudsoft.terraform.infrastructure.TerraformSshCommands;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public abstract class AbstractHandlerWorker<Steps extends Enum<?>> {

    // TODO accessors or other tidy, rather than public
    public final AmazonWebServicesClientProxy proxy;
    public final ResourceHandlerRequest<ResourceModel> request;
    public final ResourceModel model, prevModel;
    public final CallbackContext callbackContext;
    public final Logger logger;
    public final TerraformBaseHandler<CallbackContext> handler;

    // Mirror Terraform, which maxes its state checks at 10 seconds when working on long jobs
    private static final int MAX_CHECK_INTERVAL_SECONDS = 10;

    AbstractHandlerWorker(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger,
            final TerraformBaseHandler<CallbackContext> terraformBaseHandler) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        if (request.getDesiredResourceState() == null) {
            throw new IllegalArgumentException("Request model must not be null");
        }

        this.proxy = proxy;
        this.request = request;
        this.model = request.getDesiredResourceState();
        this.prevModel = request.getPreviousResourceState();
        this.callbackContext = callbackContext == null ? new CallbackContext() : callbackContext;
        this.logger = logger;
        this.handler = terraformBaseHandler;
    }

    public void log(String message) {
        System.out.println(message);
        System.out.println("<EOL>");
        logger.log(message);
    }

    public final TerraformSshCommands tfSync() {
        return TerraformSshCommands.of(this);
    }

    public final ProgressEvent<ResourceModel, CallbackContext> call() {
        logger.log(getClass().getName() + " lambda starting: " + model);

        try {
            ProgressEvent<ResourceModel, CallbackContext> result = doCall();
            logger.log(getClass().getName() + " lambda exiting, status: "+result.getStatus()+", callback: " + callbackContext);
            return result;
            
        } catch (Exception e) {
            logException(getClass().getName(), e);
            logger.log(getClass().getName() + " lambda exiting with error");
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.FAILED)
                    .build();
        }
    }

    public abstract ProgressEvent<ResourceModel, CallbackContext> doCall() throws IOException;

    // TODO put in results object
    
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

    protected final void advanceTo(Steps nextStep) {
        logger.log(String.format("advanceTo(): %s -> %s", callbackContext.stepId, nextStep));
        callbackContext.stepId = nextStep.toString();
        callbackContext.lastDelaySeconds = -1;
    }

    protected final void logException(String origin, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        logger.log(origin + " error: " + e + "\n" + sw.toString());
    }

    // This call actually consists of two network transfers, hence for large files is more
    // likely to time out. However, splitting it into two FSM states would require some place
    // to keep the downloaded file. The callback context isn't intended for that, neither is
    // the lambda's runtime filesystem.
    void getAndUploadConfiguration() throws IOException {
        tfSync().uploadConfiguration(handler.getParameters().getConfiguration(proxy, model));
    }
    
}

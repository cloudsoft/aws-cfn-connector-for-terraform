package io.cloudsoft.terraform.template.worker;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import io.cloudsoft.terraform.template.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class AbstractHandlerWorker {

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

    public final TerraformInterfaceSSH tfSync() {
        return TerraformInterfaceSSH.of(this);
    }
    
    public abstract ProgressEvent<ResourceModel, CallbackContext> call();

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

    void advanceTo(String nextStep) {
        logger.log(String.format("advanceTo(): %s -> %s", callbackContext.stepId, nextStep));
        callbackContext.stepId = nextStep;
        callbackContext.lastDelaySeconds = -1;
    }

    void logException (String origin, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        logger.log(origin + " error: " + e + "\n" + sw.toString());
    }

    void getAndUploadConfiguration() throws IOException {
        tfSync().uploadConfiguration(handler.getConfiguration(proxy, model));
    }
}

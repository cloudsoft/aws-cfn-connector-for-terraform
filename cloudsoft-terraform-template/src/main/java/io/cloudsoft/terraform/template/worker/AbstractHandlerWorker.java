package io.cloudsoft.terraform.template.worker;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import io.cloudsoft.terraform.template.*;

import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class AbstractHandlerWorker {

    final AmazonWebServicesClientProxy proxy;
    final ResourceHandlerRequest<ResourceModel> request;
    final ResourceModel model;
    final CallbackContext callbackContext;
    final Logger logger;
    final TerraformBaseHandler<CallbackContext> handler;
    final TerraformInterfaceSSH tfSync;

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
        this.callbackContext = callbackContext == null ? new CallbackContext() : callbackContext;
        this.logger = logger;
        this.handler = terraformBaseHandler;
        this.tfSync = new TerraformInterfaceSSH(terraformBaseHandler, proxy, model.getName());
    }

    public void log(String message) {
        System.out.println(message);
        System.out.println("<EOL>");
        logger.log(message);
    }

    public abstract ProgressEvent<ResourceModel, CallbackContext> call();

    int nextDelay(CallbackContext callbackContext) {
        if (callbackContext.lastDelaySeconds == 0) {
            callbackContext.lastDelaySeconds = 1;
        } else {
            if (callbackContext.lastDelaySeconds < 60) {
                // exponential backoff from 1 second up to 1 minute
                callbackContext.lastDelaySeconds *= 2;
            } else {
                callbackContext.lastDelaySeconds = 60;
            }
        }
        return callbackContext.lastDelaySeconds;
    }

    void advanceTo(String nextStep) {
        logger.log(String.format("advanceTo(): %s -> %s", callbackContext.stepId, nextStep));
        callbackContext.stepId = nextStep;
        callbackContext.lastDelaySeconds = 0;
    }

    void logException (String origin, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        logger.log(origin + " error: " + e + "\n" + sw.toString());
    }
}

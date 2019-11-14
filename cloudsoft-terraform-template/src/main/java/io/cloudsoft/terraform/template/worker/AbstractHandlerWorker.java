package io.cloudsoft.terraform.template.worker;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import io.cloudsoft.terraform.template.CallbackContext;
import io.cloudsoft.terraform.template.ResourceModel;
import io.cloudsoft.terraform.template.TerraformBaseHandler;
import io.cloudsoft.terraform.template.TerraformInterfaceSSH;

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
}

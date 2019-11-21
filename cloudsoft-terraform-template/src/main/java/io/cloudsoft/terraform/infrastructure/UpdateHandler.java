package io.cloudsoft.terraform.infrastructure;

import io.cloudsoft.terraform.infrastructure.worker.UpdateHandlerWorker;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends TerraformBaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        return run(callbackContext, cb -> new UpdateHandlerWorker(proxy, request, cb, logger, this));
    }
}

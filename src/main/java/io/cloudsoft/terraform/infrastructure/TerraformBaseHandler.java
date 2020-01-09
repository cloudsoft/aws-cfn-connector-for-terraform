package io.cloudsoft.terraform.infrastructure;

import software.amazon.cloudformation.proxy.*;

public abstract class TerraformBaseHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        if (logger==null) {
            System.err.println("No logger set.");
            throw new NullPointerException("logger");
        }

        try {
            TerraformBaseWorker<?> worker = newWorker();
            worker.init(proxy, request, callbackContext, logger);
            return worker.runHandlingError();

        } catch (Exception e) {
            e.printStackTrace();
            logger.log("Failed to create worker: "+e);
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.FAILED)
                .message(e.toString())
                .build();
        }
    }

    protected abstract TerraformBaseWorker<?> newWorker();
}

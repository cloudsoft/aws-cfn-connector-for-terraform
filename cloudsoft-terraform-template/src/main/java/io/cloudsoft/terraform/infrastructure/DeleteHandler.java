package io.cloudsoft.terraform.infrastructure;

import io.cloudsoft.terraform.infrastructure.worker.DeleteHandlerWorker;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends TerraformBaseHandler<CallbackContext> {

    public DeleteHandler(SsmClient ssmClient, S3Client s3Client) {
        super(ssmClient, s3Client);
    }

    public DeleteHandler() {
        super();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        return run(callbackContext, cb -> new DeleteHandlerWorker(proxy, request, cb, logger, this));
    }
}

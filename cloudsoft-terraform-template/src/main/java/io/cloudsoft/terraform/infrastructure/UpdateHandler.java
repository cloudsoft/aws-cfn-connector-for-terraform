package io.cloudsoft.terraform.infrastructure;

import io.cloudsoft.terraform.infrastructure.worker.UpdateHandlerWorker;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends TerraformBaseHandler<CallbackContext> {

    public UpdateHandler(SsmClient ssmClient, S3Client s3Client) {
        super(ssmClient, s3Client);
    }

    public UpdateHandler() {
        super();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        return run(callbackContext, cb -> new UpdateHandlerWorker(proxy, request, cb, logger, this));
    }
}

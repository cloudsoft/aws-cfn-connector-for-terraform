package io.cloudsoft.terraform.template;

import io.cloudsoft.terraform.template.worker.CreateHandlerWorker;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends TerraformBaseHandler<CallbackContext> {

    // for tests
    public CreateHandler(SsmClient ssmClient, S3Client s3Client) {
        super(ssmClient, s3Client);
    }

    public CreateHandler() {
        super();
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        return run(callbackContext, cb -> new CreateHandlerWorker(proxy, request, cb, logger, this));
    }
}

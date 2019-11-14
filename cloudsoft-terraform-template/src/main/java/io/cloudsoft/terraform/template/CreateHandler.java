package io.cloudsoft.terraform.template;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import io.cloudsoft.terraform.template.worker.CreateHandlerWorker;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ssm.SsmClient;

public class CreateHandler extends TerraformBaseHandler<CallbackContext> {

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

    public enum Steps {
        INIT,
        SYNC_MKDIR,
        SYNC_DOWNLOAD,
        ASYNC_TF_INIT,
        ASYNC_TF_APPLY,
        DONE
    }
}

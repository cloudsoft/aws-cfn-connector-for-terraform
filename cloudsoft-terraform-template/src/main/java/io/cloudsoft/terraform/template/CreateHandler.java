package io.cloudsoft.terraform.template;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import io.cloudsoft.terraform.template.worker.CreateHandlerWorker;

public class CreateHandler extends TerraformBaseHandler<CallbackContext> {

    public CreateHandler(AWSSimpleSystemsManagement awsSimpleSystemsManagement, AmazonS3 amazonS3) {
        super(awsSimpleSystemsManagement, amazonS3);
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

        return run(callbackContext, cb -> new CreateHandlerWorker(request, cb, logger, this));
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

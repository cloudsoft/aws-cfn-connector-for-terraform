package io.cloudsoft.terraform.infrastructure.worker;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.CallbackContext;
import io.cloudsoft.terraform.infrastructure.RemoteSystemdUnit;
import io.cloudsoft.terraform.infrastructure.ResourceModel;
import io.cloudsoft.terraform.infrastructure.UpdateHandler;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandlerWorker extends AbstractHandlerWorker<UpdateHandlerWorker.Steps> {
    
    protected enum Steps {
        UPDATE_INIT,
        UPDATE_SYNC_UPLOAD,
        UPDATE_ASYNC_TF_APPLY,
        UPDATE_DONE
    }

    public UpdateHandlerWorker(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger,
            final UpdateHandler updateHandler) {
        super(proxy, request, callbackContext, logger, updateHandler);
    }

    public ProgressEvent<ResourceModel, CallbackContext> doCall() throws IOException {
        Steps curStep = callbackContext.stepId == null ? Steps.UPDATE_INIT : Steps.valueOf(callbackContext.stepId);
        RemoteSystemdUnit tfApply = RemoteSystemdUnit.of(this, "terraform-apply");
        switch (curStep) {
            case UPDATE_INIT:
                advanceTo(Steps.UPDATE_SYNC_UPLOAD);
                getAndUploadConfiguration();
                return inProgressResult();

            case UPDATE_SYNC_UPLOAD:
                advanceTo(Steps.UPDATE_ASYNC_TF_APPLY);
                tfApply.start();
                return inProgressResult();

            case UPDATE_ASYNC_TF_APPLY:
                if (tfApply.isRunning()) {
                    return inProgressResult();
                }
                if (tfApply.wasFailure()) {
                    // TODO log stdout/stderr
                    logger.log("ERROR: " + tfApply.getLog());
                    // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                    throw new IOException("tfApply returned errno " + tfApply.getErrno() + " / '" + tfApply.getResult() + "'");
                }
                advanceTo(Steps.UPDATE_DONE);
                return inProgressResult();

            case UPDATE_DONE:
                return success();
                
            default:
                throw new IllegalStateException("Invalid step: " + callbackContext.stepId);
        }
    }

}

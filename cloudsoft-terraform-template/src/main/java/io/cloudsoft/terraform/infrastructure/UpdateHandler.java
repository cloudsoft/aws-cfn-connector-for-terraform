package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;

import software.amazon.cloudformation.proxy.ProgressEvent;

public class UpdateHandler extends TerraformBaseHandler<UpdateHandler.Steps> {

    protected enum Steps {
        UPDATE_INIT,
        UPDATE_SYNC_UPLOAD,
        UPDATE_ASYNC_TF_APPLY,
        UPDATE_DONE
    }
    
    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException {
        Steps curStep = callbackContext.stepId == null ? Steps.UPDATE_INIT : Steps.valueOf(callbackContext.stepId);
        switch (curStep) {
            case UPDATE_INIT:
                advanceTo(Steps.UPDATE_SYNC_UPLOAD);
                getAndUploadConfiguration();
                return progressEvents().inProgressResult();

            case UPDATE_SYNC_UPLOAD:
                advanceTo(Steps.UPDATE_ASYNC_TF_APPLY);
                tfApply().start();
                return progressEvents().inProgressResult();

            case UPDATE_ASYNC_TF_APPLY:
                if (tfApply().isRunning()) {
                    return progressEvents().inProgressResult();
                }
                if (tfApply().wasFailure()) {
                    // TODO log stdout/stderr
                    logger.log("ERROR: " + tfApply().getLog());
                    // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                    throw new IOException("tfApply returned errno " + tfApply().getErrno() + " / '" + tfApply().getResult() + "'");
                }
                advanceTo(Steps.UPDATE_DONE);
                return progressEvents().inProgressResult();

            case UPDATE_DONE:
                return progressEvents().success();
                
            default:
                throw new IllegalStateException("Invalid step: " + callbackContext.stepId);
        }
    }


}

package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;

import software.amazon.cloudformation.proxy.ProgressEvent;

public class UpdateHandler extends TerraformBaseHandler {

    protected enum Steps {
        UPDATE_SYNC_FILE,
        UPDATE_RUN_TF_APPLY,
        UPDATE_WAIT_ON_APPLY_THEN_RETURN
    }
    
    @Override
    protected TerraformBaseWorker<?> newWorker() {
        return new Worker();
    }
    
    protected static class Worker extends TerraformBaseWorker<Steps> {
        
        @Override
        protected ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException {
            Steps curStep = callbackContext.stepId == null ? Steps.UPDATE_SYNC_FILE : Steps.valueOf(callbackContext.stepId);
            switch (curStep) {
                case UPDATE_SYNC_FILE:
                    getAndUploadConfiguration();
                    advanceTo(Steps.UPDATE_RUN_TF_APPLY);
                    return progressEvents().inProgressResult();
    
                case UPDATE_RUN_TF_APPLY:
                    advanceTo(Steps.UPDATE_WAIT_ON_APPLY_THEN_RETURN);
                    tfApply().start();
                    return progressEvents().inProgressResult();
    
                case UPDATE_WAIT_ON_APPLY_THEN_RETURN:
                    if (tfApply().isRunning()) {
                        return progressEvents().inProgressResult();
                    }
                    if (tfApply().wasFailure()) {
                        // TODO log stdout/stderr
                        logger.log("ERROR: " + tfApply().getLog());
                        // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                        throw new IOException("tfApply returned errno " + tfApply().getErrno() + " / '" + tfApply().getResult() + "'");
                    }
                    
                    return progressEvents().success();
                    
                default:
                    throw new IllegalStateException("Invalid step: " + callbackContext.stepId);
            }
        }
    
    }
    
}
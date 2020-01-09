package io.cloudsoft.terraform.infrastructure;

import software.amazon.cloudformation.proxy.ProgressEvent;

import java.io.IOException;

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
            currentStep = callbackContext.stepId == null ? Steps.UPDATE_SYNC_FILE : Steps.valueOf(callbackContext.stepId);

            switch (currentStep) {
                case UPDATE_SYNC_FILE:
                    getAndUploadConfiguration();
                    advanceTo(Steps.UPDATE_RUN_TF_APPLY);
                    return progressEvents().inProgressResult();

                case UPDATE_RUN_TF_APPLY:
                    advanceTo(Steps.UPDATE_WAIT_ON_APPLY_THEN_RETURN);
                    tfApply().start();
                    return progressEvents().inProgressResult();

                case UPDATE_WAIT_ON_APPLY_THEN_RETURN:
                    if (checkStillRunnningOrError(tfApply())) {
                        return progressEvents().inProgressResult();
                    }

                    return progressEvents().success();

                default:
                    throw new IllegalStateException("Invalid step: " + callbackContext.stepId);
            }
        }

    }

}
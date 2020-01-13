package io.cloudsoft.terraform.infrastructure;

import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.io.IOException;

public class DeleteHandler extends TerraformBaseHandler {

    protected enum Steps {
        DELETE_RUN_TF_DESTROY,
        DELETE_WAIT_ON_DESTROY_THEN_RMDIR_AND_RETURN
    }

    @Override
    protected TerraformBaseWorker<?> newWorker() {
        return new Worker();
    }

    protected static class Worker extends TerraformBaseWorker<Steps> {

        @Override
        protected ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException {
            currentStep = callbackContext.stepId == null ? Steps.DELETE_RUN_TF_DESTROY : Steps.valueOf(callbackContext.stepId);
            switch (currentStep) {
                case DELETE_RUN_TF_DESTROY:
                    advanceTo(Steps.DELETE_WAIT_ON_DESTROY_THEN_RMDIR_AND_RETURN);
                    tfDestroy().start();
                    return statusInProgress();

                case DELETE_WAIT_ON_DESTROY_THEN_RMDIR_AND_RETURN:
                    if (checkStillRunningOrError(tfDestroy())) {
                        return statusInProgress();
                    }

                    tfSshCommands().rmdir();

                    return statusSuccess();
                default:
                    throw new IllegalStateException("invalid step " + callbackContext.stepId);
            }
        }

    }
}
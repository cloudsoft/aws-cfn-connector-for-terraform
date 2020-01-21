package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.commands.RemoteTerraformProcess;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class DeleteHandler extends TerraformBaseHandler {

    private enum Steps {
        DELETE_RUN_TF_DESTROY,
        DELETE_WAIT_ON_DESTROY_THEN_RMDIR_AND_RETURN
    }

    @Override
    protected TerraformBaseWorker<?> newWorker() {
        return new Worker();
    }

    protected static class Worker extends TerraformBaseWorker<Steps> {

        public Worker() { super(Steps.class); }
        
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

                    RemoteTerraformProcess.of(this).rmWorkDir();

                    return statusSuccess();
                default:
                    throw new IllegalStateException("invalid step " + callbackContext.stepId);
            }
        }

    }
}

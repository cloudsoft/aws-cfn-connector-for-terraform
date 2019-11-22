package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.commands.RemoteSystemdUnit;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

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
            // TODO tidy
            RemoteSystemdUnit tfDestroy = tfDestroy();
            switch (currentStep) {
                case DELETE_RUN_TF_DESTROY:
                    advanceTo(Steps.DELETE_WAIT_ON_DESTROY_THEN_RMDIR_AND_RETURN);
                    tfDestroy.start();
                    return progressEvents().inProgressResult();
    
                case DELETE_WAIT_ON_DESTROY_THEN_RMDIR_AND_RETURN:
                    if (tfDestroy.isRunning()) {
                        return progressEvents().inProgressResult();
                    }
                    if (tfDestroy.wasFailure()) {
                        // TODO log stdout/stderr
                        logger.log("ERROR: " + tfDestroy.getLog());
                        // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                        throw new IOException("tfDestroy returned errno " + tfDestroy.getErrno() + " / '" + tfDestroy.getResult() + "' / " + tfDestroy.getLastExitStatusOrNull());
                    }
    
                    tfSync().rmdir();
                    
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModel(model)
                            .status(OperationStatus.SUCCESS)
                            .build();
                default:
                    throw new IllegalStateException("invalid step " + callbackContext.stepId);
            }
        }

    }
}

package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.commands.RemoteSystemdUnit;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class DeleteHandler extends TerraformBaseHandler<DeleteHandler.Steps> {
    
    protected enum Steps {
        DELETE_INIT,
        DELETE_ASYNC_TF_DESTROY,
        DELETE_SYNC_RMDIR,
        DELETE_DONE
    }
    
    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException {
        Steps curStep = callbackContext.stepId == null ? Steps.DELETE_INIT : Steps.valueOf(callbackContext.stepId);
        RemoteSystemdUnit tfDestroy = tfDestroy();
        switch (curStep) {
            case DELETE_INIT:
                advanceTo(Steps.DELETE_ASYNC_TF_DESTROY);
                tfDestroy.start();
                return progressEvents().inProgressResult();

            case DELETE_ASYNC_TF_DESTROY:
                if (tfDestroy.isRunning()) {
                    return progressEvents().inProgressResult();
                }
                if (tfDestroy.wasFailure()) {
                    // TODO log stdout/stderr
                    logger.log("ERROR: " + tfDestroy.getLog());
                    // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                    throw new IOException("tfDestroy returned errno " + tfDestroy.getErrno() + " / '" + tfDestroy.getResult() + "' / " + tfDestroy.getLastExitStatusOrNull());
                }
                advanceTo(Steps.DELETE_SYNC_RMDIR);
                return progressEvents().inProgressResult();

            case DELETE_SYNC_RMDIR:
                advanceTo(Steps.DELETE_DONE);
                tfSync().rmdir();
                return progressEvents().inProgressResult();

            case DELETE_DONE:
                logger.log(getClass().getName() + " completed: success");
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.SUCCESS)
                        .build();
            default:
                throw new IllegalStateException("invalid step " + callbackContext.stepId);
        }
    }

}

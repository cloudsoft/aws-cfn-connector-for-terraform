package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.commands.RemoteTerraformProcess;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class DeleteHandler extends TerraformBaseHandler {

    /** TODO it might be nice if we detect failure and can delete if it isn't a failure,
     * and then possibly to expose this is a parameter "always", "never", "on_success" */
    private static boolean DELETE_LOGS = false;
    
    private enum Steps {
        DELETE_RUN_TF_DESTROY,
        DELETE_WAIT_ON_DESTROY_THEN_RMDIR_AND_RETURN
    }

    @Override
    protected TerraformBaseWorker<?> newWorker() {
        return new Worker();
    }

    protected static class Worker extends TerraformBaseWorker<Steps> {

        public Worker() { super("Delete", Steps.class); }
        
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

                    if (DELETE_LOGS) {
                        if (callbackContext.logBucketName != null) {
                            try {
                                new BucketUtils(proxy).deleteBucket(callbackContext.logBucketName);
                                log(String.format("Deleted bucket for logs at s3://%s/", callbackContext.logBucketName));
                                callbackContext.logBucketName = null;
                                setModelLogBucketUrlFromCallbackContextName();
                                
                            } catch (Exception e) {
                                String message = String.format("Failed to delete log bucket %s: %s (%s)", callbackContext.logBucketName, e.getClass().getName(), e.getMessage());
                                log(message);
                                throw ConnectorHandlerFailures.handled(message+". "+
                                    "The terraform-deployed infrastructure has been destroyed, "
                                    + "but the log bucket will need manual removal.");
                            }
                        }
                    }
                    
                    return statusSuccess();
                default:
                    throw new IllegalStateException("invalid step " + callbackContext.stepId);
            }
        }

    }
}

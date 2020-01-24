package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.commands.RemoteTerraformOutputsProcess;
import io.cloudsoft.terraform.infrastructure.commands.RemoteTerraformProcess;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class CreateHandler extends TerraformBaseHandler {

    private enum Steps {
        CREATE_LOG_TARGET,
        CREATE_INIT_AND_MKDIR,
        CREATE_SYNC_FILE,
        CREATE_RUN_TF_INIT,
        CREATE_WAIT_ON_INIT_THEN_RUN_TF_APPLY,
        CREATE_WAIT_ON_APPLY_THEN_GET_OUTPUTS_AND_RETURN
    }

    @Override
    protected TerraformBaseWorker<?> newWorker() {
        return new Worker();
    }

    protected static class Worker extends TerraformBaseWorker<Steps> {

        public Worker() { super("Create", Steps.class); }
        
        @Override
        protected void initLogBucket() {
            // do nothing during pre-run.  we _create_ the bucket in the first step of the actual run.
        }
        
        @Override
        protected void preRunStep() {
            if (model.getIdentifier()==null) {
                if (callbackContext.createdModelIdentifier == null) {
                    // creating this stack, the very first call for the stack
                    callbackContext.createdModelIdentifier = Configuration.getIdentifier(true,  8);
                    log("Identifier created: "+callbackContext.createdModelIdentifier);
                }
                // model doesn't seem to remember the identifier until the end
                model.setIdentifier(callbackContext.createdModelIdentifier);
            }
            
            super.preRunStep();
        }

        @SuppressWarnings("fallthrough")
        @Override
        protected ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException {
            switch (currentStep) {
                case CREATE_LOG_TARGET:
                    boolean creatingLogTarget = (callbackContext.logBucketName==null);
                    initLogBucketName();
                    if (creatingLogTarget) {
                        try {
                            // try writing to it, in case it already exists
                            initLogBucketFirstMessage();
                        } catch (Exception e) {
                            // try creating it
                            try {
                                new BucketUtils(proxy).createBucket(callbackContext.logBucketName);
                                log(String.format("Created bucket for logs at s3://%s/", callbackContext.logBucketName));
                                setModelLogBucketUrlFromCallbackContextName();
                                
                            } catch (Exception e2) {
                                log(String.format("Failed to create log bucket %s: %s (%s)", callbackContext.logBucketName, e2.getClass().getName(), e2.getMessage()));
                                creatingLogTarget = false;
                                callbackContext.logBucketName = null;
                                setModelLogBucketUrlFromCallbackContextName();
                            }
                        }
                    }
                    advanceTo(Steps.CREATE_INIT_AND_MKDIR);
                    if (callbackContext.logBucketName!=null) {
                        /* NOTE: here, and in several other places, we could proceed to the next
                         * step, but returning often increases transparency and maximises the time
                         * available for each step (avoiding errors due to timeout)
                         */
                        return statusInProgress();
                    }
                    
                case CREATE_INIT_AND_MKDIR:
                    RemoteTerraformProcess.of(this).mkWorkDir();
                    advanceTo(Steps.CREATE_SYNC_FILE);

                    return statusInProgress();

                case CREATE_SYNC_FILE:
                    getAndUploadConfiguration(true);
                    advanceTo(Steps.CREATE_RUN_TF_INIT);
                    return statusInProgress();

                case CREATE_RUN_TF_INIT:
                    tfInit().start();
                    advanceTo(Steps.CREATE_WAIT_ON_INIT_THEN_RUN_TF_APPLY);
                    return statusInProgress();

                case CREATE_WAIT_ON_INIT_THEN_RUN_TF_APPLY:
                    if (checkStillRunningOrError(tfInit())) {
                        return statusInProgress();
                    }

                    tfApply().start();
                    advanceTo(Steps.CREATE_WAIT_ON_APPLY_THEN_GET_OUTPUTS_AND_RETURN);
                    return statusInProgress();

                case CREATE_WAIT_ON_APPLY_THEN_GET_OUTPUTS_AND_RETURN:
                    if (checkStillRunningOrError(tfApply())) {
                        return statusInProgress();
                    }

                    RemoteTerraformOutputsProcess outputCmd = RemoteTerraformOutputsProcess.of(this);
                    outputCmd.run();
                    model.setOutputsStringified(outputCmd.getOutputAsJsonStringized());
                    model.setOutputs(outputCmd.getOutputAsMap());

                    return statusSuccess();

                default:
                    throw new IllegalStateException("Invalid step: " + callbackContext.stepId);
            }
        }

    }

}

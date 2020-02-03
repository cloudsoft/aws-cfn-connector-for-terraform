package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.commands.RemoteTerraformOutputsProcess;
import io.cloudsoft.terraform.infrastructure.commands.RemoteTerraformProcess;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class CreateHandler extends TerraformBaseHandler {

    private enum Steps {
        CREATE_LOG_TARGET,
        CREATE_INIT_AND_UPLOAD,
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
                    model.setIdentifier(callbackContext.createdModelIdentifier);
                    
                    log("Stack resource model identifier set as: "+callbackContext.createdModelIdentifier);
                    
                    // create the metadata file
                    saveMetadata();
                    
                } else {
                    // model doesn't seem to remember the identifier until the end
                    model.setIdentifier(callbackContext.createdModelIdentifier);
                }
            }
            
            super.preRunStep();
        }

        @SuppressWarnings("fallthrough")
        @Override
        protected ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException {
            switch (currentStep) {
                case CREATE_LOG_TARGET:
                    String logBucketName = model.getLogBucketName();
                    if (logBucketName==null) {
                        logBucketName = getParameters().getLogsS3BucketPrefix();
                        if (logBucketName!=null) {
                            logBucketName = logBucketName + "-" + model.getIdentifier().toLowerCase();
                        }
                    }
                    boolean triedCreatingLogBucket = false;
                    if (logBucketName!=null) {
                        callbackContext.logBucketName = logBucketName;
                        setModelLogBucketUrlFromCallbackContextName();

                        // try writing, in case it exists
                        if (!initLogBucketFirstMessage()) {
                            // try creating it -- but first restore the name (as failed write will have reset it)
                            callbackContext.logBucketName = logBucketName;
                            setModelLogBucketUrlFromCallbackContextName();
                            
                            log("Log bucket "+logBucketName+" does not exist or is not accessible (there may be related failure messages above); will try to create it");
                            try {
                                triedCreatingLogBucket = true;
                                new BucketUtils(proxy).createBucket(logBucketName);
                                if (!initLogBucketFirstMessage()) {
                                    throw new IllegalStateException("Bucket created but we cannot write to it. Check permissions. Log bucket will be disabled.");
                                }
                                log(String.format("Created bucket for logs at s3://%s/", logBucketName));
                                setModelLogBucketUrlFromCallbackContextName();
                            } catch (Exception e) {
                                log(String.format("Failed to create log bucket %s: %s (%s)", logBucketName, e.getClass().getName(), e.getMessage()));
                                callbackContext.logBucketName = null;
                                model.setLogBucketName(null);  // clear the log bucket they requested
                                setModelLogBucketUrlFromCallbackContextName();
                            }
                        }
                        saveMetadata();
                    }
                    
                    advanceTo(Steps.CREATE_INIT_AND_UPLOAD);
                    if (triedCreatingLogBucket) {
                        /* NOTE: here, and in several other places, we could always proceed to the next
                         * step, but returning often increases transparency and maximises the time
                         * available for each step (avoiding errors due to timeout), so do that if
                         * we've done things on a step that might have taken a bit of time
                         */
                        return statusInProgress();
                    }
                    
                case CREATE_INIT_AND_UPLOAD:
                    RemoteTerraformProcess.of(this).mkWorkDir();
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

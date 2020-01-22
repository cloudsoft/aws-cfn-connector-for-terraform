package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.commands.RemoteTerraformOutputsProcess;
import io.cloudsoft.terraform.infrastructure.commands.RemoteTerraformProcess;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
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

        public Worker() { super(Steps.class); }
        
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
                    if (creatingLogTarget) {
                        callbackContext.logBucketName = parameters.getLogsS3BucketPrefix();
                        if (callbackContext.logBucketName!=null) {
                            callbackContext.logBucketName += "-" + model.getIdentifier();
                        }
                        final S3Client s3Client = S3Client.create();
                        CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                                .bucket(callbackContext.logBucketName)
                                .build();
                        try {
                            proxy.injectCredentialsAndInvokeV2(createBucketRequest, request -> s3Client.createBucket(createBucketRequest));
                            log(String.format("Created bucket for logs at s3://%s/", callbackContext.logBucketName));
                            setModelLogBucketUrlFromCallbackContextName();
                        } catch (Exception e) {
                            log(String.format("Failed to create log bucket %s: %s (%s)", callbackContext.logBucketName, e.getClass().getName(), e.getMessage()));
                            creatingLogTarget = false;
                            callbackContext.logBucketName = null;
                            model.setLogBucketUrl(null);
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

package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;
import java.util.UUID;

import io.cloudsoft.terraform.infrastructure.commands.RemoteTerraformProcess;
import io.cloudsoft.terraform.infrastructure.commands.RemoteTerraformOutputsProcess;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class CreateHandler extends TerraformBaseHandler {

    protected enum Steps {
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

        @Override
        protected ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException {
            if (callbackContext.createdModelIdentifier == null) {
                callbackContext.createdModelIdentifier = UUID.randomUUID().toString();
            }
            model.setIdentifier(callbackContext.createdModelIdentifier);

            String processManager = getParameters().getProcessManager();
            if (callbackContext.processManager == null) {
                callbackContext.processManager = processManager;
            }
            switch (processManager) {
                case "systemd":
                    break;
                default:
                    // Belt and braces -- getProcessManager() is supposed to do all required validation.
                    throw new IllegalStateException ("Invalid process manager configured: " + processManager);
            }

            currentStep = getCallbackContext().stepId == null ? Steps.CREATE_INIT_AND_MKDIR : Steps.valueOf(callbackContext.stepId);

            switch (currentStep) {
                case CREATE_INIT_AND_MKDIR:
                    RemoteTerraformProcess.of(this).mkWorkDir();
                    advanceTo(Steps.CREATE_SYNC_FILE);

                    /* NOTE: here, and in several other places, we could proceed to the next
                     * step, but returning often increases transparency and maximises the time
                     * available for each step (avoiding errors due to timeout)
                     */
                    return statusInProgress();

                case CREATE_SYNC_FILE:
                    getAndUploadConfiguration();
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

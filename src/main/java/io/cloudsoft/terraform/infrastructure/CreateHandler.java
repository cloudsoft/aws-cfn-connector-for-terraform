package io.cloudsoft.terraform.infrastructure;

import io.cloudsoft.terraform.infrastructure.commands.TerraformOutputsCommand;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.io.IOException;
import java.util.UUID;

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

            currentStep = getCallbackContext().stepId == null ? Steps.CREATE_INIT_AND_MKDIR : Steps.valueOf(callbackContext.stepId);

            switch (currentStep) {
                case CREATE_INIT_AND_MKDIR:
                    tfSshCommands().mkdir();
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
                    if (checkStillRunnningOrError(tfInit())) {
                        return statusInProgress();
                    }

                    tfApply().start();
                    advanceTo(Steps.CREATE_WAIT_ON_APPLY_THEN_GET_OUTPUTS_AND_RETURN);
                    return statusInProgress();

                case CREATE_WAIT_ON_APPLY_THEN_GET_OUTPUTS_AND_RETURN:
                    if (checkStillRunnningOrError(tfApply())) {
                        return statusInProgress();
                    }

                    TerraformOutputsCommand outputCmd = TerraformOutputsCommand.of(this);
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

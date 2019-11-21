package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;
import java.util.UUID;

import io.cloudsoft.terraform.infrastructure.commands.RemoteSystemdUnit;
import io.cloudsoft.terraform.infrastructure.commands.TerraformOutputsCommand;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class CreateHandler extends TerraformBaseHandler<CreateHandler.Steps> {

    protected enum Steps {
        CREATE_INIT_AND_MKDIR,
        CREATE_SYNC_CONFIG,
        CREATE_RUN_TF_INIT,
        CREATE_WAIT_ON_INIT_THEN_RUN_TF_APPLY,
        CREATE_WAIT_ON_APPLY_THEN_GET_OUTPUTS_AND_RETURN,
        CREATE_GET_OUTPUTS,
        CREATE_DONE
    }

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException {
        if (callbackContext.createdModelIdentifier == null) {
            callbackContext.createdModelIdentifier = UUID.randomUUID().toString();
        }
        model.setIdentifier(callbackContext.createdModelIdentifier);

        Steps currentStep = callbackContext.stepId == null ? Steps.CREATE_INIT_AND_MKDIR : Steps.valueOf(callbackContext.stepId);
        
        switch (currentStep) {
            case CREATE_INIT_AND_MKDIR:
                tfSync().mkdir();
                advanceTo(Steps.CREATE_SYNC_CONFIG);
                
                /* NOTE: here, and in several other places, we could proceed to the next
                 * step, but returning often increases transparency and maximises the time 
                 * available for each step (avoiding errors due to timeout)
                 */
                return progressEvents().inProgressResult();

            case CREATE_SYNC_CONFIG:
                getAndUploadConfiguration();
                advanceTo(Steps.CREATE_RUN_TF_INIT);
                return progressEvents().inProgressResult();

            case CREATE_RUN_TF_INIT:
                tfInit().start();
                advanceTo(Steps.CREATE_WAIT_ON_INIT_THEN_RUN_TF_APPLY);
                return progressEvents().inProgressResult();

            case CREATE_WAIT_ON_INIT_THEN_RUN_TF_APPLY:
                RemoteSystemdUnit tfInit = tfInit();
                if (tfInit.isRunning()) {
                    return progressEvents().inProgressResult();
                }
                if (tfInit.wasFailure()) {
                    // TODO log stdout/stderr
                    logger.log("ERROR: " + tfInit.getLog());
                    // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                    throw new IOException("tfInit returned errno " + tfInit.getErrno() + " / '" + tfInit.getResult() + "' / " + tfInit.getLastExitStatusOrNull());
                }
                
                tfApply().start();
                advanceTo(Steps.CREATE_WAIT_ON_APPLY_THEN_GET_OUTPUTS_AND_RETURN);
                return progressEvents().inProgressResult();

            case CREATE_WAIT_ON_APPLY_THEN_GET_OUTPUTS_AND_RETURN:
                RemoteSystemdUnit tfApply = tfApply();
                if (tfApply.isRunning()) {
                    return progressEvents().inProgressResult();
                }
                if (tfApply.wasFailure()) {
                    // TODO log stdout/stderr
                    logger.log("ERROR: " + tfApply.getLog());
                    // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                    throw new IOException("tfDestroy returned errno " + tfApply.getErrno() + " / '" + tfApply.getResult() + "' / " + tfApply.getLastExitStatusOrNull());
                }

                TerraformOutputsCommand outputCmd = TerraformOutputsCommand.of(this);
                outputCmd.run();
                model.setOutputsStringified(outputCmd.getOutputAsJsonStringized());
                model.setOutputs(outputCmd.getOutputAsMap());
                advanceTo(Steps.CREATE_DONE);

                return progressEvents().success();
                
            default:
                throw new IllegalStateException("Invalid step: " + callbackContext.stepId);
        }
    }

}

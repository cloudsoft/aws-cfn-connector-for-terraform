package io.cloudsoft.terraform.infrastructure.worker;

import java.io.IOException;
import java.util.UUID;

import io.cloudsoft.terraform.infrastructure.CallbackContext;
import io.cloudsoft.terraform.infrastructure.CreateHandler;
import io.cloudsoft.terraform.infrastructure.RemoteSystemdUnit;
import io.cloudsoft.terraform.infrastructure.ResourceModel;
import io.cloudsoft.terraform.infrastructure.TerraformOutputsCommand;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandlerWorker extends AbstractHandlerWorker<CreateHandlerWorker.Steps> {
    
    protected enum Steps {
        CREATE_INIT,
        CREATE_SYNC_MKDIR,
        CREATE_SYNC_UPLOAD,
        CREATE_ASYNC_TF_INIT,
        CREATE_ASYNC_TF_APPLY,
        CREATE_GET_OUTPUTS,
        CREATE_DONE
    }

    public CreateHandlerWorker(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger,
            final CreateHandler createHandler) {
        super(proxy, request, callbackContext, logger, createHandler);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> doCall() throws IOException {
        if (callbackContext.createdModelIdentifier == null) {
            callbackContext.createdModelIdentifier = UUID.randomUUID().toString();
        }
        model.setIdentifier(callbackContext.createdModelIdentifier);

        Steps currentStep = callbackContext.stepId == null ? Steps.CREATE_INIT : Steps.valueOf(callbackContext.stepId);
        
        switch (currentStep) {
            case CREATE_INIT:
                advanceTo(Steps.CREATE_SYNC_MKDIR);
                tfSync().mkdir();
                
                /* NOTE: here, and in several other places, we could proceed to the next
                 * step, but returning often increases transparency and maximises the time 
                 * available for each step (avoiding errors due to timeout)
                 */
                return inProgressResult();

            case CREATE_SYNC_MKDIR:
                advanceTo(Steps.CREATE_SYNC_UPLOAD);
                getAndUploadConfiguration();
                return inProgressResult();

            case CREATE_SYNC_UPLOAD:
                advanceTo(Steps.CREATE_ASYNC_TF_INIT);
                tfInit().start();
                return inProgressResult();

            case CREATE_ASYNC_TF_INIT:
                RemoteSystemdUnit tfInit = tfInit();
                if (tfInit.isRunning()) {
                    return inProgressResult();
                }
                if (tfInit.wasFailure()) {
                    // TODO log stdout/stderr
                    logger.log("ERROR: " + tfInit.getLog());
                    // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                    throw new IOException("tfInit returned errno " + tfInit.getErrno() + " / '" + tfInit.getResult() + "' / " + tfInit.getLastExitStatusOrNull());
                }
                
                tfApply().start();
                advanceTo(Steps.CREATE_ASYNC_TF_APPLY);
                return inProgressResult();

            case CREATE_ASYNC_TF_APPLY:
                RemoteSystemdUnit tfApply = tfApply();
                if (tfApply.isRunning()) {
                    return inProgressResult();
                }
                if (tfApply.wasFailure()) {
                    // TODO log stdout/stderr
                    logger.log("ERROR: " + tfApply.getLog());
                    // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                    throw new IOException("tfDestroy returned errno " + tfApply.getErrno() + " / '" + tfApply.getResult() + "' / " + tfApply.getLastExitStatusOrNull());
                }
                advanceTo(Steps.CREATE_GET_OUTPUTS);
                return inProgressResult();

            case CREATE_GET_OUTPUTS:
                TerraformOutputsCommand outputCmd = TerraformOutputsCommand.of(this);
                outputCmd.run();
                model.setOutputsStringified(outputCmd.getOutputAsJsonStringized());
                model.setOutputs(outputCmd.getOutputAsMap());
                advanceTo(Steps.CREATE_DONE);
                // TODO removee
                return inProgressResult();

            case CREATE_DONE:
                return success();
                
            default:
                throw new IllegalStateException("Invalid step: " + callbackContext.stepId);
        }
    }


    protected RemoteSystemdUnit tfApply() {
        return RemoteSystemdUnit.of(this, "terraform-apply");
    }

    protected RemoteSystemdUnit tfInit() {
        return RemoteSystemdUnit.of(this, "terraform-init");
    }

}

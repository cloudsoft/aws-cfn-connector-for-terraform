package io.cloudsoft.terraform.template.worker;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import io.cloudsoft.terraform.template.CallbackContext;
import io.cloudsoft.terraform.template.CreateHandler;
import io.cloudsoft.terraform.template.RemoteSystemdUnit;
import io.cloudsoft.terraform.template.ResourceModel;
import io.cloudsoft.terraform.template.TerraformOutputsCommand;

import java.io.IOException;
import java.util.UUID;

public class CreateHandlerWorker extends AbstractHandlerWorker {
    public enum Steps {
        CREATE_INIT,
        CREATE_SYNC_MKDIR,
        CREATE_SYNC_UPLOAD,
        CREATE_ASYNC_TF_INIT,
        CREATE_ASYNC_TF_APPLY,
        GET_OUTPUTS,
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

    public ProgressEvent<ResourceModel, CallbackContext> call() {
        logger.log(getClass().getName() + " lambda starting: " + model);

        try {
            if (model.getIdentifier()==null) {
                model.setIdentifier(UUID.randomUUID().toString());
            }
            Steps curStep = callbackContext.stepId == null ? Steps.CREATE_INIT : Steps.valueOf(callbackContext.stepId);
            RemoteSystemdUnit tfInit = RemoteSystemdUnit.of(this, "terraform-init");
            RemoteSystemdUnit tfApply = RemoteSystemdUnit.of(this, "terraform-apply");
            switch (curStep) {
                case CREATE_INIT:
                    advanceTo(Steps.CREATE_SYNC_MKDIR);
                    tfSync().onlyMkdir();
                    break;   // don't _need_ to break here, but improves readability,
                             // helps us maximize the time for each step (avoid timeout), and
                             // in any case the framework calls back quickly 
                case CREATE_SYNC_MKDIR:
                    advanceTo(Steps.CREATE_SYNC_UPLOAD);
                    getAndUploadConfiguration();
                    break;   // optional break, as above
                case CREATE_SYNC_UPLOAD:
                    advanceTo(Steps.CREATE_ASYNC_TF_INIT);
                    tfInit.start();
                    break;   // optional break, as above
                    
                case CREATE_ASYNC_TF_INIT:
                    if (tfInit.isRunning()) {
                        break; // return IN_PROGRESS
                    }
                    if (tfInit.wasFailure()) {
                        // TODO log stdout/stderr
                        logger.log("ERROR: "+tfInit.getLog());
                        // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                        throw new IOException("tfInit returned errno " + tfInit.getErrno() + " / '"+tfInit.getResult()+"' / "+tfInit.getLastExitStatusOrNull());
                    }
                    advanceTo(Steps.CREATE_ASYNC_TF_APPLY);
                    tfApply.start();
                    break;   // optional break, as above
                    
                case CREATE_ASYNC_TF_APPLY:
                    if (tfApply.isRunning()) {
                        break; // return IN_PROGRESS
                    }
                    if (tfApply.wasFailure()) {
                        // TODO log stdout/stderr
                        logger.log("ERROR: "+tfApply.getLog());
                        // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                        throw new IOException("tfDestroy returned errno " + tfApply.getErrno() + " / '"+tfApply.getResult()+"' / "+tfApply.getLastExitStatusOrNull());
                    }
                    advanceTo(Steps.GET_OUTPUTS);
                    break;   // optional break, as above
                    
                case GET_OUTPUTS:
                    TerraformOutputsCommand outputCmd = TerraformOutputsCommand.of(this);
                    outputCmd.run();
                    model.setOutputsStringified(outputCmd.getOutputAsJsonStringized());
                    model.setOutputs(outputCmd.getOutputAsMap());
                    advanceTo(Steps.CREATE_DONE);
                    // no need to break
                    
                case CREATE_DONE:
                    logger.log(getClass().getName() + " completed: success");
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModel(model)
                            .status(OperationStatus.SUCCESS)
                            .build();
                default:
                    throw new IllegalStateException("invalid step " + callbackContext.stepId);
            }
        } catch (Exception e) {
            logException (getClass().getName(), e);
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.FAILED)
                    .build();
        }

        logger.log(getClass().getName() + " lambda exiting, callback: " + callbackContext);
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .callbackContext(callbackContext)
                .callbackDelaySeconds(nextDelay(callbackContext))
                .status(OperationStatus.IN_PROGRESS)
                .build();
    }

    private void advanceTo(Steps nextStep) {
        super.advanceTo(nextStep.toString());
    }
}

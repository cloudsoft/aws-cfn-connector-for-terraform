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

import java.io.IOException;

public class CreateHandlerWorker extends AbstractHandlerWorker {
    public enum Steps {
        CREATE_INIT,
        CREATE_SYNC_MKDIR,
        CREATE_SYNC_UPLOAD,
        CREATE_ASYNC_TF_INIT,
        CREATE_ASYNC_TF_APPLY,
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
            Steps curStep = callbackContext.stepId == null ? Steps.CREATE_INIT : Steps.valueOf(callbackContext.stepId);
            RemoteSystemdUnit tfInit = new RemoteSystemdUnit(this.handler, this.proxy, "terraform-init", model.getName());
            RemoteSystemdUnit tfApply = new RemoteSystemdUnit(this.handler, this.proxy, "terraform-apply", model.getName());
            switch (curStep) {
                case CREATE_INIT:
                    advanceTo(Steps.CREATE_SYNC_MKDIR);
                    tfSync.onlyMkdir();
                    break;
                case CREATE_SYNC_MKDIR:
                    advanceTo(Steps.CREATE_SYNC_UPLOAD);
                    getAndUploadConfiguration();
                    break;
                case CREATE_SYNC_UPLOAD:
                    advanceTo(Steps.CREATE_ASYNC_TF_INIT);
                    tfInit.start();
                    break;
                case CREATE_ASYNC_TF_INIT:
                    if (tfInit.isRunning())
                        break; // return IN_PROGRESS
                    if (tfInit.wasFailure())
                        throw new IOException("tfInit returned errno " + tfInit.getErrno());
                    advanceTo(Steps.CREATE_ASYNC_TF_APPLY);
                    tfApply.start();
                    break;
                case CREATE_ASYNC_TF_APPLY:
                    if (tfApply.isRunning())
                        break; // return IN_PROGRESS
                    if (tfApply.wasFailure())
                        throw new IOException("tfApply returned errno " + tfApply.getErrno());
                    advanceTo(Steps.CREATE_DONE);
                    break;
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

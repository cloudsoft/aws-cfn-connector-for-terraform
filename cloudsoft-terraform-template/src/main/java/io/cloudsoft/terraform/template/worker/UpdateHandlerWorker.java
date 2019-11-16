package io.cloudsoft.terraform.template.worker;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import io.cloudsoft.terraform.template.CallbackContext;
import io.cloudsoft.terraform.template.UpdateHandler;
import io.cloudsoft.terraform.template.RemoteSystemdUnit;
import io.cloudsoft.terraform.template.ResourceModel;

import java.io.IOException;

public class UpdateHandlerWorker extends AbstractHandlerWorker {
    public enum Steps {
        UPDATE_INIT,
        UPDATE_SYNC_UPLOAD,
        UPDATE_ASYNC_TF_APPLY,
        UPDATE_DONE
    }

    public UpdateHandlerWorker(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger,
            final UpdateHandler updateHandler) {
        super(proxy, request, callbackContext, logger, updateHandler);
    }

    public ProgressEvent<ResourceModel, CallbackContext> call() {
        logger.log(getClass().getName() + " lambda starting: " + model);

        try {
            Steps curStep = callbackContext.stepId == null ? Steps.UPDATE_INIT : Steps.valueOf(callbackContext.stepId);
            RemoteSystemdUnit tfApply = RemoteSystemdUnit.of(this, "terraform-apply");
            switch (curStep) {
                case UPDATE_INIT:
                    advanceTo(Steps.UPDATE_SYNC_UPLOAD);
                    getAndUploadConfiguration();
//                    break;
                    
                case UPDATE_SYNC_UPLOAD:
                    advanceTo(Steps.UPDATE_ASYNC_TF_APPLY);
                    tfApply.start();
//                    break;
                    
                case UPDATE_ASYNC_TF_APPLY:
                    if (tfApply.isRunning()) {
                        break; // return IN_PROGRESS
                    }
                    if (tfApply.wasFailure()) {
                        // TODO log stdout/stderr
                        logger.log("ERROR: "+tfApply.getLog());
                        // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                        throw new IOException("tfApply returned errno " + tfApply.getErrno() + " / '"+tfApply.getResult()+"'");
                    }
                    advanceTo(Steps.UPDATE_DONE);
                    break;
                case UPDATE_DONE:
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

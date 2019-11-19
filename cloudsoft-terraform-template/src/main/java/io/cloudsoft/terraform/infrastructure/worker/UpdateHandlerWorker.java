package io.cloudsoft.terraform.infrastructure.worker;

import io.cloudsoft.terraform.infrastructure.CallbackContext;
import io.cloudsoft.terraform.infrastructure.RemoteSystemdUnit;
import io.cloudsoft.terraform.infrastructure.ResourceModel;
import io.cloudsoft.terraform.infrastructure.UpdateHandler;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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
                    break;   // optional break, as per CreateHandlerWorker

                case UPDATE_SYNC_UPLOAD:
                    advanceTo(Steps.UPDATE_ASYNC_TF_APPLY);
                    tfApply.start();
                    break;   // optional break, as per CreateHandlerWorker

                case UPDATE_ASYNC_TF_APPLY:
                    if (tfApply.isRunning()) {
                        break; // return IN_PROGRESS
                    }
                    if (tfApply.wasFailure()) {
                        // TODO log stdout/stderr
                        logger.log("ERROR: " + tfApply.getLog());
                        // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                        throw new IOException("tfApply returned errno " + tfApply.getErrno() + " / '" + tfApply.getResult() + "'");
                    }
                    advanceTo(Steps.UPDATE_DONE);
                    // no need to break

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
            logException(getClass().getName(), e);
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

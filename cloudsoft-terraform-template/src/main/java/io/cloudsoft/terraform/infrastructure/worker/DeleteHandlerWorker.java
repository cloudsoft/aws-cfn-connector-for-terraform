package io.cloudsoft.terraform.infrastructure.worker;

import io.cloudsoft.terraform.infrastructure.CallbackContext;
import io.cloudsoft.terraform.infrastructure.DeleteHandler;
import io.cloudsoft.terraform.infrastructure.RemoteSystemdUnit;
import io.cloudsoft.terraform.infrastructure.ResourceModel;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.io.IOException;

public class DeleteHandlerWorker extends AbstractHandlerWorker {
    private enum Steps {
        DELETE_INIT,
        DELETE_ASYNC_TF_DESTROY,
        DELETE_SYNC_RMDIR,
        DELETE_DONE
    }

    public DeleteHandlerWorker(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger,
            final DeleteHandler deleteHandler) {
        super(proxy, request, callbackContext, logger, deleteHandler);
    }

    public ProgressEvent<ResourceModel, CallbackContext> call() {
        logger.log(getClass().getName() + " lambda starting: " + model);

        try {
            Steps curStep = callbackContext.stepId == null ? Steps.DELETE_INIT : Steps.valueOf(callbackContext.stepId);
            RemoteSystemdUnit tfDestroy = RemoteSystemdUnit.of(this, "terraform-destroy");
            switch (curStep) {
                case DELETE_INIT:
                    advanceTo(Steps.DELETE_ASYNC_TF_DESTROY);
                    tfDestroy.start();
                    break;   // optional break, as per CreateHandlerWorker

                case DELETE_ASYNC_TF_DESTROY:
                    if (tfDestroy.isRunning()) {
                        break; // return IN_PROGRESS
                    }
                    if (tfDestroy.wasFailure()) {
                        // TODO log stdout/stderr
                        logger.log("ERROR: " + tfDestroy.getLog());
                        // TODO make this a new "AlreadyLoggedException" where we suppress the trace
                        throw new IOException("tfDestroy returned errno " + tfDestroy.getErrno() + " / '" + tfDestroy.getResult() + "' / " + tfDestroy.getLastExitStatusOrNull());
                    }
                    advanceTo(Steps.DELETE_SYNC_RMDIR);
                    break;   // optional break, as per CreateHandlerWorker

                case DELETE_SYNC_RMDIR:
                    advanceTo(Steps.DELETE_DONE);
                    tfSync().onlyRmdir();
                    // no need to break

                case DELETE_DONE:
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

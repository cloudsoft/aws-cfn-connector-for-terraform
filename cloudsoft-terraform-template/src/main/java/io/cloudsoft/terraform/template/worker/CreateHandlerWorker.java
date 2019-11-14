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
import java.io.PrintWriter;
import java.io.StringWriter;

public class CreateHandlerWorker extends AbstractHandlerWorker {

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
            CreateHandler.Steps curStep = callbackContext.stepId == null ? CreateHandler.Steps.INIT : CreateHandler.Steps.valueOf(callbackContext.stepId);
            RemoteSystemdUnit tfInit = new RemoteSystemdUnit(this.handler,  this.proxy, "terraform-init", model.getName());
            RemoteSystemdUnit tfApply = new RemoteSystemdUnit(this.handler, this.proxy, "terraform-apply", model.getName());
            switch (curStep) {
                case INIT:
                    advanceTo(CreateHandler.Steps.SYNC_MKDIR);
                    tfSync.onlyMkdir();
                    break;
                case SYNC_MKDIR:
                    advanceTo(CreateHandler.Steps.SYNC_DOWNLOAD);
                    tfSync.onlyDownload(model.getConfigurationUrl());
                    break;
                case SYNC_DOWNLOAD:
                    advanceTo(CreateHandler.Steps.ASYNC_TF_INIT);
                    tfInit.start();
                    break;
                case ASYNC_TF_INIT:
                    if (tfInit.isRunning())
                        break; // return IN_PROGRESS
                    if (tfInit.wasFailure())
                        throw new IOException("tfInit returned errno " + tfInit.getErrno());
                    advanceTo(CreateHandler.Steps.ASYNC_TF_APPLY);
                    tfApply.start();
                    break;
                case ASYNC_TF_APPLY:
                    if (tfApply.isRunning())
                        break; // return IN_PROGRESS
                    if (tfApply.wasFailure())
                        throw new IOException("tfApply returned errno " + tfApply.getErrno());
                    advanceTo(CreateHandler.Steps.DONE);
                    break;
                case DONE:
                    logger.log(getClass().getName() + " completed: success");
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModel(model)
                            .status(OperationStatus.SUCCESS)
                            .build();
                default:
                    throw new IllegalStateException("invalid step " + callbackContext.stepId);
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logger.log(getClass().getName() + " error: " + e + "\n" + sw.toString());
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

    private int nextDelay(CallbackContext callbackContext) {
        if (callbackContext.lastDelaySeconds == 0) {
            callbackContext.lastDelaySeconds = 1;
        } else {
            if (callbackContext.lastDelaySeconds < 60) {
                // exponential backoff from 1 second up to 1 minute
                callbackContext.lastDelaySeconds *= 2;
            } else {
                callbackContext.lastDelaySeconds = 60;
            }
        }
        return callbackContext.lastDelaySeconds;
    }

    private void advanceTo(CreateHandler.Steps nextStep) {
        logger.log(String.format("advanceTo(): %s -> %s", callbackContext.stepId, nextStep.toString()));
        callbackContext.stepId = nextStep.toString();
        callbackContext.lastDelaySeconds = 0;
    }
}

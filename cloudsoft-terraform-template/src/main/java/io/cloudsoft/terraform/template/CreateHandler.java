package io.cloudsoft.terraform.template;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends TerraformBaseHandler<CallbackContext> {
    
    enum Steps {
        INIT,
        SYNC_MKDIR,
        SYNC_DOWNLOAD,
        ASYNC_TF_INIT,
        ASYNC_TF_APPLY,
        DONE
    }
        
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        
        return run(callbackContext, cb -> new Worker(request, cb, logger));
    }
    
    class Worker extends AbstractHandlerWorker {

        protected Worker(
                final ResourceHandlerRequest<ResourceModel> request,
                final CallbackContext callbackContext,
                final Logger logger) {
            super(request, callbackContext, logger);
        }
        
        public ProgressEvent<ResourceModel, CallbackContext> call() {
            logger.log("CreateHandler lambda starting: "+model);
            
            if (TEST_RETURN_SUCCESS_IMMEDIATELY) {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();
            }

            try {
                Steps curStep = callbackContext.stepId == null ? Steps.INIT : Steps.valueOf(callbackContext.stepId);
                TerraformInterfaceSSH tfSync = new TerraformInterfaceSSH(CreateHandler.this, model.getName());
                RemoteSystemdUnit tfInit = new RemoteSystemdUnit(CreateHandler.this, "terraform-init", model.getName());
                RemoteSystemdUnit tfApply = new RemoteSystemdUnit(CreateHandler.this, "terraform-apply", model.getName());
                switch (curStep) {
                    case INIT:
                        advanceTo(Steps.SYNC_MKDIR);
                        tfSync.onlyMkdir();
                        break;
                    case SYNC_MKDIR:
                        advanceTo(Steps.SYNC_DOWNLOAD);
                        tfSync.onlyDownload(model.getConfigurationUrl());
                        break;
                    case SYNC_DOWNLOAD:
                        advanceTo(Steps.ASYNC_TF_INIT);
                        tfInit.start();
                        break;
                    case ASYNC_TF_INIT:
                        if (tfInit.isRunning()) {
                            log("DEBUG: waiting in ASYNC_TF_INIT");
                            break; // return IN_PROGRESS
                        }
                        if (!tfInit.wasSuccess())
                            throw new IOException("tfInit returned errno " + tfInit.getErrno());
                        advanceTo(Steps.ASYNC_TF_APPLY);
                        tfApply.start();
                        break;
                    case ASYNC_TF_APPLY:
                        if (tfApply.isRunning())
                        {
                            log("DEBUG: waiting in ASYNC_TF_APPLY");
                            break; // return IN_PROGRESS
                        }
                        if (! tfApply.wasSuccess())
                            throw new IOException ("tfApply returned errno " + tfApply.getErrno());
                        advanceTo(Steps.DONE);
                        break;
                    case DONE:
                        logger.log("CreateHandler completed: success");
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
                logger.log("CreateHandler error: " + e + "\n" + sw.toString());
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.FAILED)
                        .build();
            }
            
            logger.log("CreateHandler lambda exiting, callback: "+callbackContext);
            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .callbackContext(callbackContext)
                    .callbackDelaySeconds(nextDelay(callbackContext))
                    .status(OperationStatus.IN_PROGRESS)
                    .build();
        }

        private int nextDelay(CallbackContext callbackContext) {
            if (callbackContext.lastDelaySeconds==0) {
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
    
        private void advanceTo(Steps nextStep) {
            logger.log (String.format("advanceTo(): %s -> %s", callbackContext.stepId, nextStep.toString()));
            callbackContext.stepId = nextStep.toString();
            callbackContext.lastDelaySeconds = 0;
        }
    }
    
}

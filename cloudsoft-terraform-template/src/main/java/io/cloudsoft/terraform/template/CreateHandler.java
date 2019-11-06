package io.cloudsoft.terraform.template;

import java.io.IOException;
import java.util.function.Function;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends TerraformBaseHandler<CallbackContext> {

    enum Steps {
        INSTALL_PLAN, APPLY_PLAN, SET_OUTPUTS
    }
    
    // visible for testing
    Function<CallbackContext, AsyncSshHelper> asyncSshHelperFactory = cb -> new AsyncSshHelper(cb);
    
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext,
            final Logger logger) {
        
        return new Worker(request, callbackContext, logger).run();
    }
    
    protected class Worker {

        final ResourceHandlerRequest<ResourceModel> request;
        private ResourceModel model;
        final CallbackContext callbackContext;
        final Logger logger;
        private AsyncSshHelper asyncSshHelper;
        
        protected Worker(
                final ResourceHandlerRequest<ResourceModel> request,
                final CallbackContext callbackContext,
                final Logger logger) {
            
            this.request = request;
            this.model = request.getDesiredResourceState();
            this.callbackContext = callbackContext==null ? new CallbackContext() : callbackContext;
            this.logger = logger;
            this.asyncSshHelper = asyncSshHelperFactory.apply(this.callbackContext);
        }
        
        public ProgressEvent<ResourceModel, CallbackContext> run() {

            if (callbackContext.sessionId == null) {
                // TODO better session ID
                callbackContext.sessionId = "session"+System.currentTimeMillis();
    
                installPlan(request, callbackContext, logger);
                
            } else if (!asyncSshHelper.checkFinishedAndUpdate()) {
                // still running last command, no op
                
            } else {
                // TODO check if succeeded or failed; for now assume success
                
                switch (Steps.valueOf(callbackContext.stepId)) {
                
                case INSTALL_PLAN:
                    // TODO check asyncSshHelper.stdout, stderr okay
                    
                    applyPlan();
                    break;
                    
                case APPLY_PLAN:
                    // TODO check asyncSshHelper.stdout, stderr okay
                    setOutputs();
                    break;
                    
                case SET_OUTPUTS:
                    // TODO check asyncSshHelper.stdout, stderr okay
                    // TODO store output
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.SUCCESS)
                        .build();
                }
            }
            
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
            callbackContext.stepId = nextStep.toString();
            callbackContext.lastDelaySeconds = 0;
            callbackContext.pid = 0;
        }
        
        private void installPlan(ResourceHandlerRequest<ResourceModel> request, CallbackContext callbackContext, Logger logger) {
            advanceTo(Steps.INSTALL_PLAN);
            // TODO fix the command
            asyncSshHelper.runOrRejoinAndSetPid("wget xxxx");
        }
        
        private void applyPlan() {
            advanceTo(Steps.APPLY_PLAN);
            
            // TODO for now keep the synchronous approach also
            String cfg = getConfiguration(model);
            if (cfg!=null && !cfg.isEmpty()) {
                try  {
                    TerraformInterfaceSSH tfif = new TerraformInterfaceSSH(CreateHandler.this, model.getName());
                    tfif.createTemplateFromURL(model.getConfigurationUrl());
                    // TODO really should, for now, do:
//                    tfif.createTemplateFromContents(cfg);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }

            // TODO fix the command
            asyncSshHelper.runOrRejoinAndSetPid("wget xxxx");
        }
    
        private void setOutputs() {
            advanceTo(Steps.SET_OUTPUTS);
            // TODO fix the command
            // TODO - note this should probably be synchronous
            asyncSshHelper.runOrRejoinAndSetPid("TODO get outputs");
        }

    }
    
}

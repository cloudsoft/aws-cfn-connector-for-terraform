package io.cloudsoft.terraform.template;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandler<CallbackContext> {

    enum Steps {
        INSTALL_PLAN, APPLY_PLAN, SET_OUTPUTS
    }
    
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        
        ResourceModel model = request.getDesiredResourceState();
        
        if (callbackContext.sessionId == null) {
            installPlan(request, callbackContext, logger);
        } else {
            AsyncSshHelper helper = new AsyncSshHelper(callbackContext);
            switch (Steps.valueOf(callbackContext.stepId)) {
            case INSTALL_PLAN:
                if (helper.update(callbackContext.pid)) {
                    // TODO check if succeeded or failed; for now assume success
                    applyPlan(request, callbackContext, logger);
                } else {
                    // not finished yet, keep waiting
                }
                break;
                
            case APPLY_PLAN:
                if (helper.update(callbackContext.pid)) {
                    // TODO check if succeeded or failed; for now assume success
                    setOutputs(request, callbackContext, logger);
                } else {
                    // not finished yet, keep waiting
                }
                break;
                
            case SET_OUTPUTS:
                if (helper.update(callbackContext.pid)) {
                    // TODO check if succeeded or failed; for now assume success
                    // TODO store output
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .resourceModel(model)
                        .status(OperationStatus.SUCCESS)
                        .build();
                    
                } else {
                    // not finished yet, keep waiting
                }
                break;
                
            default:
                break;
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
            }
        }
        // reset every time step changes
        return 5;
    }

    private void installPlan(ResourceHandlerRequest<ResourceModel> request, CallbackContext callbackContext, Logger logger) {
        // TODO better session ID
        callbackContext.sessionId = "session"+System.currentTimeMillis();
        callbackContext.stepId = Steps.INSTALL_PLAN.toString();
        callbackContext.lastDelaySeconds = 0;
        
        // TODO
        callbackContext.pid = new AsyncSshHelper(callbackContext).runOrRejoin("wget xxxx");
    }
    
    private void applyPlan(ResourceHandlerRequest<ResourceModel> request, CallbackContext callbackContext, Logger logger) {
        callbackContext.stepId = Steps.APPLY_PLAN.toString();
        callbackContext.lastDelaySeconds = 0;
        
        // TODO
        callbackContext.pid = new AsyncSshHelper(callbackContext).runOrRejoin("terraform apply ...");
    }

    private void setOutputs(ResourceHandlerRequest<ResourceModel> request, CallbackContext callbackContext, Logger logger) {
        callbackContext.stepId = Steps.SET_OUTPUTS.toString();
        callbackContext.lastDelaySeconds = 0;
        
        // TODO - notee this should bee synchronous
        callbackContext.pid = new AsyncSshHelper(callbackContext).runOrRejoin("TODO get outputs");
    }

}

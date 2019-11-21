package io.cloudsoft.terraform.infrastructure;

import com.google.common.base.Preconditions;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public abstract class TerraformBaseHandler extends BaseHandler<CallbackContext> {

    // TODO accessors or other tidy, rather than public
    public AmazonWebServicesClientProxy proxy;
    public ResourceHandlerRequest<ResourceModel> request;
    public ResourceModel model, prevModel;
    public CallbackContext callbackContext;
    public Logger logger;
    public TerraformBaseHandler handler;
    
    TerraformParameters parameters;
    
    protected void init(AmazonWebServicesClientProxy proxy, ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext, Logger logger) {
        this.proxy = proxy;
        this.request = request;
        this.callbackContext = callbackContext;
        this.logger = logger;
        
        // TODO replace with 'this'
        this.handler = this;
    }

    public synchronized TerraformParameters getParameters() {
        if (parameters==null) {
            parameters = new TerraformParameters();
        }
        return parameters;
    }
    public synchronized void setParameters(TerraformParameters parameters) {
        if (this.parameters!=null) {
            throw new IllegalStateException("Handler can only be setup and used once, and parameters have already initialized when attempting to re-initializee them");
        }
        this.parameters = Preconditions.checkNotNull(parameters, "parameters");
    }
    
    
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        
        init(proxy, request, callbackContext, logger);
        return runWithLoopingIfNecessary();
    }
    
    // TODO: This is now obsolete as the cfn-cli handles reinvokes out of the box. Should remove it.
    protected ProgressEvent<ResourceModel, CallbackContext> runWithLoopingIfNecessary() {
        // allows us to force synchronous behaviour -- especially useful when running in SAM
        boolean forceSynchronous = callbackContext == null ? false : callbackContext.forceSynchronous;
        boolean disregardCallbackDelay = callbackContext == null ? false : callbackContext.disregardCallbackDelay;

        while (true) {
            ProgressEvent<ResourceModel, CallbackContext> result = run();
            if (!forceSynchronous || !OperationStatus.IN_PROGRESS.equals(result.getStatus())) {
                return result;
            }
            log("Synchronous mode: " + result.getCallbackContext());
            try {
                if (disregardCallbackDelay) {
                    log("Will run callback immediately");
                } else {
                    log("Will run callback after " + result.getCallbackDelaySeconds() + " seconds");
                    Thread.sleep(1000 * result.getCallbackDelaySeconds());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Aborted due to interrupt", e);
            }
            callbackContext = result.getCallbackContext();
        }
    }
    
    public void log(String message) {
        System.out.println(message);
        System.out.println("<EOL>");
        logger.log(message);
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> run();
        
}

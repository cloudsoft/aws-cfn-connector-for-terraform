package io.cloudsoft.terraform.infrastructure;

import java.util.function.Function;

import com.google.common.base.Preconditions;

import io.cloudsoft.terraform.infrastructure.worker.AbstractHandlerWorker;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

public abstract class TerraformBaseHandler<T> extends BaseHandler<T> {

    TerraformParameters parameters;
    
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
    
    // TODO: This is now obsolete as the cfn-cli handles reinvokes out of the box. Should remove it.
    protected ProgressEvent<ResourceModel, CallbackContext> run(CallbackContext callback, Function<CallbackContext, AbstractHandlerWorker<?>> workerFactory) {
        // allows us to force synchronous behaviour -- especially useful when running in SAM
        boolean forceSynchronous = callback == null ? false : callback.forceSynchronous;
        boolean disregardCallbackDelay = callback == null ? false : callback.disregardCallbackDelay;

        while (true) {
            AbstractHandlerWorker<?> worker = workerFactory.apply(callback);
            ProgressEvent<ResourceModel, CallbackContext> result = worker.call();
            if (!forceSynchronous || !OperationStatus.IN_PROGRESS.equals(result.getStatus())) {
                return result;
            }
            worker.log("Synchronous mode: " + result.getCallbackContext());
            try {
                if (disregardCallbackDelay) {
                    worker.log("Will run callback immediately");
                } else {
                    worker.log("Will run callback after " + result.getCallbackDelaySeconds() + " seconds");
                    Thread.sleep(1000 * result.getCallbackDelaySeconds());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Aborted due to interrupt", e);
            }
            callback = result.getCallbackContext();
        }
    }
}

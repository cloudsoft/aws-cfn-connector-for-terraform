package io.cloudsoft.terraform.infrastructure;

import io.cloudsoft.terraform.infrastructure.worker.DeleteHandlerWorker;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class DeleteHandler extends TerraformBaseHandler {
    
    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> run() {
        return new DeleteHandlerWorker(proxy, request, callbackContext, logger, this).call();
    }

}

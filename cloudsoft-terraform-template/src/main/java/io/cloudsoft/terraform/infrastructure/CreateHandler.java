package io.cloudsoft.terraform.infrastructure;

import io.cloudsoft.terraform.infrastructure.worker.CreateHandlerWorker;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class CreateHandler extends TerraformBaseHandler {

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> run() {
        return new CreateHandlerWorker(proxy, request, callbackContext, logger, this).call();
    }

}

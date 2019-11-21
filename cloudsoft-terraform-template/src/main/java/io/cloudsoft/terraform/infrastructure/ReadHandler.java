package io.cloudsoft.terraform.infrastructure;

import io.cloudsoft.terraform.infrastructure.worker.ReadHandlerWorker;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class ReadHandler extends TerraformBaseHandler {

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> run() {
        return new ReadHandlerWorker(proxy, request, callbackContext, logger, this).call();
    }

}

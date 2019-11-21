package io.cloudsoft.terraform.infrastructure;

import io.cloudsoft.terraform.infrastructure.worker.UpdateHandlerWorker;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class UpdateHandler extends TerraformBaseHandler {

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> run() {
        return new UpdateHandlerWorker(proxy, request, callbackContext, logger, this).call();
    }

}

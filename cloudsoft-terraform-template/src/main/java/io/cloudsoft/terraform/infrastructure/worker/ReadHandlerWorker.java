package io.cloudsoft.terraform.infrastructure.worker;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.CallbackContext;
import io.cloudsoft.terraform.infrastructure.ReadHandler;
import io.cloudsoft.terraform.infrastructure.ResourceModel;
import io.cloudsoft.terraform.infrastructure.TerraformOutputsCommand;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandlerWorker extends AbstractHandlerWorker<ReadHandlerWorker.NoSteps> {

    protected enum NoSteps {}
    
    public ReadHandlerWorker(AmazonWebServicesClientProxy proxy, ResourceHandlerRequest<ResourceModel> request, CallbackContext callbackContext, Logger logger, ReadHandler handler) {
        super(proxy, request, callbackContext, logger, handler);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> doCall() throws IOException {
        TerraformOutputsCommand outputCmd = TerraformOutputsCommand.of(this);
        outputCmd.run();
        model.setOutputsStringified(outputCmd.getOutputAsJsonStringized());
        model.setOutputs(outputCmd.getOutputAsMap());
    
        return success();
    }
}

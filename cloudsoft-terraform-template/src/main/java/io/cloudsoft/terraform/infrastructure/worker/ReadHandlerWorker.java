package io.cloudsoft.terraform.infrastructure.worker;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.CallbackContext;
import io.cloudsoft.terraform.infrastructure.ResourceModel;
import io.cloudsoft.terraform.infrastructure.TerraformBaseHandler;
import io.cloudsoft.terraform.infrastructure.TerraformOutputsCommand;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandlerWorker extends AbstractHandlerWorker<ReadHandlerWorker.NoSteps> {

    protected enum NoSteps {}
    
    public ReadHandlerWorker(AmazonWebServicesClientProxy proxy, ResourceHandlerRequest<ResourceModel> request, CallbackContext callbackContext, Logger logger, TerraformBaseHandler<CallbackContext> terraformBaseHandler) {
        super(proxy, request, callbackContext, logger, terraformBaseHandler);
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

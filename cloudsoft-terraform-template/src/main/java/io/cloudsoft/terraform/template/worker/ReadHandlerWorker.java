package io.cloudsoft.terraform.template.worker;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;

import io.cloudsoft.terraform.template.CallbackContext;
import io.cloudsoft.terraform.template.ResourceModel;
import io.cloudsoft.terraform.template.TerraformBaseHandler;
import io.cloudsoft.terraform.template.TerraformOutputsCommand;

public class ReadHandlerWorker extends AbstractHandlerWorker {

    public ReadHandlerWorker(AmazonWebServicesClientProxy proxy, ResourceHandlerRequest<ResourceModel> request, CallbackContext callbackContext, Logger logger, TerraformBaseHandler<CallbackContext> terraformBaseHandler) {
        super(proxy, request, callbackContext, logger, terraformBaseHandler);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> call() {
        OperationStatus status = OperationStatus.SUCCESS;

        logger.log("ReadHandlerWorker desired model = "+model+"; prevModel = "+prevModel);
        
        try {
            TerraformOutputsCommand outputCmd = TerraformOutputsCommand.of(this);
            outputCmd.run();
            model.setOutputsStringified(outputCmd.getOutputAsJsonStringized());
            model.setOutputs(outputCmd.getOutputAsMap());
            
        } catch (Exception e) {
            logException("ReadHandlerWorker", e);
            status = OperationStatus.FAILED;
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(status)
                .build();
    }
}

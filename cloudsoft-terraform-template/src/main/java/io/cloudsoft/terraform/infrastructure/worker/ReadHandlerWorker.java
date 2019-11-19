package io.cloudsoft.terraform.infrastructure.worker;

import io.cloudsoft.terraform.infrastructure.CallbackContext;
import io.cloudsoft.terraform.infrastructure.ResourceModel;
import io.cloudsoft.terraform.infrastructure.TerraformBaseHandler;
import io.cloudsoft.terraform.infrastructure.TerraformOutputsCommand;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandlerWorker extends AbstractHandlerWorker {

    public ReadHandlerWorker(AmazonWebServicesClientProxy proxy, ResourceHandlerRequest<ResourceModel> request, CallbackContext callbackContext, Logger logger, TerraformBaseHandler<CallbackContext> terraformBaseHandler) {
        super(proxy, request, callbackContext, logger, terraformBaseHandler);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> call() {
        OperationStatus status = OperationStatus.SUCCESS;

        logger.log("ReadHandlerWorker desired model = " + model + "; prevModel = " + prevModel);

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

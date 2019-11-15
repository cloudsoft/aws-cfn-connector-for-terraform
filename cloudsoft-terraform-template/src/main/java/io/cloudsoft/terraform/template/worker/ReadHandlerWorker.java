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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ReadHandlerWorker extends AbstractHandlerWorker {

    public ReadHandlerWorker(AmazonWebServicesClientProxy proxy, ResourceHandlerRequest<ResourceModel> request, CallbackContext callbackContext, Logger logger, TerraformBaseHandler<CallbackContext> terraformBaseHandler) {
        super(proxy, request, callbackContext, logger, terraformBaseHandler);
    }

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> call() {
        TerraformOutputsCommand tfOutputsCommand = new TerraformOutputsCommand(this.handler, this.proxy, this.model.getName());
        OperationStatus status = OperationStatus.SUCCESS;

        logger.log("ReadHandlerWorker desired model = "+model+"; prevModel = "+prevModel);
        
        try {
            this.model.setOutputs(tfOutputsCommand.run(logger));
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logger.log("ReadHandlerWorker error: " + e + "\n" + sw.toString());
            status = OperationStatus.FAILED;
        }

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(status)
                .build();
    }
}

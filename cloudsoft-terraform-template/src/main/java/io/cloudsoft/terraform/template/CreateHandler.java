package io.cloudsoft.terraform.template;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;

import java.io.IOException;

public class CreateHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {
        ResourceModel model = request.getDesiredResourceState();
        OperationStatus ret = OperationStatus.PENDING;
        try {
            TerraformInterfaceSSH tfif = new TerraformInterfaceSSH(model.getTFServerName(), model.getTFInfrastructureName());
            tfif.createTemplateFromURL(model.getTFConfigurationURL());
            ret = OperationStatus.SUCCESS;
        } catch (IOException e) {
            ret = OperationStatus.FAILED;
        }
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(ret)
                .build();
    }
}

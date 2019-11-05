package io.cloudsoft.terraform.template;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.DeleteMetricFilterRequest;

import java.io.IOException;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        ResourceModel model = request.getDesiredResourceState();

        OperationStatus ret = OperationStatus.PENDING;
        try {
            TerraformInterfaceSSH tfif = new TerraformInterfaceSSH("localhost", "template1");
            tfif.deleteTemplate();
            ret = OperationStatus.SUCCESS;
        } catch (IOException e) {
            ret = OperationStatus.FAILED;
        }

        // Convert model to a form that works with your API
        DeleteMetricFilterRequest deleteMetricFilterRequest = new
                DeleteMetricFilterRequest()
                .withFilterName(model.getFilterName())
                .withLogGroupName(model.getLogGroupName());

        // Initialize client and send request
        AWSLogs client = AWSLogsClientBuilder.standard().withRegion("eu-central-1").build();
        // This is a minimal example, so we've kept it easy with no error handling
        // You should add more error handling in real-world handlers
        proxy.injectCredentialsAndInvoke(deleteMetricFilterRequest,
                client::deleteMetricFilter);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(ret)
                .build();
    }
}

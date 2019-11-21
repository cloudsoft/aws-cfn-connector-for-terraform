package io.cloudsoft.terraform.infrastructure;

import java.util.ArrayList;
import java.util.List;

import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class ListHandler extends TerraformBaseHandler {
    
    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> run() {
        final List<ResourceModel> models = new ArrayList<>();
        
        // TODO handler not implemented yet
        
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModels(models)
            .status(OperationStatus.SUCCESS)
            .build();
    }

}

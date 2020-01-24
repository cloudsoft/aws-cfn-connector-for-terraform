package io.cloudsoft.terraform.infrastructure;

import java.util.ArrayList;
import java.util.List;

import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class ListHandler extends TerraformBaseHandler {

    private enum Steps {
        // none
    }

    @Override
    protected TerraformBaseWorker<?> newWorker() {
        return new Worker();
    }

    protected static class Worker extends TerraformBaseWorker<Steps> {

        public Worker() { super(Steps.class); }
        
        @Override
        protected ProgressEvent<ResourceModel, CallbackContext> runStep() {
            final List<ResourceModel> models = new ArrayList<>();

            // handler not used yet, always return empty

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
        }

    }

}
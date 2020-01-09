package io.cloudsoft.terraform.infrastructure;

import io.cloudsoft.terraform.infrastructure.CreateHandler.Steps;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.util.ArrayList;
import java.util.List;

public class ListHandler extends TerraformBaseHandler {

    @Override
    protected TerraformBaseWorker<?> newWorker() {
        return new Worker();
    }

    protected static class Worker extends TerraformBaseWorker<Steps> {

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
package io.cloudsoft.terraform.infrastructure;

import io.cloudsoft.terraform.infrastructure.CreateHandler.Steps;
import io.cloudsoft.terraform.infrastructure.commands.RemoteTerraformOutputsProcess;
import software.amazon.cloudformation.proxy.ProgressEvent;

import java.io.IOException;

public class ReadHandler extends TerraformBaseHandler {

    @Override
    protected TerraformBaseWorker<?> newWorker() {
        return new Worker();
    }

    protected static class Worker extends TerraformBaseWorker<Steps> {

        @Override
        protected ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException {
            RemoteTerraformOutputsProcess outputCmd = RemoteTerraformOutputsProcess.of(this);
            outputCmd.run();
            model.setOutputsStringified(outputCmd.getOutputAsJsonStringized());
            model.setOutputs(outputCmd.getOutputAsMap());

            return statusSuccess();
        }

    }

}
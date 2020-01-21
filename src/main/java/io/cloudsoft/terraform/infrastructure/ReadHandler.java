package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.commands.RemoteTerraformOutputsProcess;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class ReadHandler extends TerraformBaseHandler {

    private enum Steps {
        // nonoe
    }

    @Override
    protected TerraformBaseWorker<?> newWorker() {
        return new Worker();
    }

    protected static class Worker extends TerraformBaseWorker<Steps> {

        public Worker() { super(Steps.class); }
        
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
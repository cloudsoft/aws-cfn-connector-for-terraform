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

        public Worker() { super("Read", Steps.class); }
        
        @Override
        protected ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException {
            log("Read requested, given desired model "+model+" and previous model "+prevModel);
            
            // copy previous values for those we don't look up dynamically
            if (prevModel!=null) {
                model.setIdentifier(prevModel.getIdentifier());
                model.setConfigurationContent(prevModel.getConfigurationContent());
                model.setConfigurationS3Path(prevModel.getConfigurationS3Path());
                model.setConfigurationUrl(prevModel.getConfigurationUrl());
                model.setLogBucketUrl(prevModel.getLogBucketUrl());
                model.setVariables(prevModel.getVariables());
            }

            // and look up these to make sure they are current
            RemoteTerraformOutputsProcess outputCmd = RemoteTerraformOutputsProcess.of(this);
            outputCmd.run();
            model.setOutputsStringified(outputCmd.getOutputAsJsonStringized());
            model.setOutputs(outputCmd.getOutputAsMap());
            
            return statusSuccess();
        }

    }

}

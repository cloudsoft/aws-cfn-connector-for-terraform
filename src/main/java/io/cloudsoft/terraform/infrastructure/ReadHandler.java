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
            
            // this is set by the framework
//            model.setIdentifier
            
            // these are set by the call to loadMetadata
//            model.setLogBucketName
//            model.setLogBucketUrl
            
            // these are left null by the current implementation
            // (we could cache them as part of metadata)
//            model.setConfigurationContent(prevModel.getConfigurationContent());
//            model.setConfigurationS3Path(prevModel.getConfigurationS3Path());
//            model.setConfigurationUrl(prevModel.getConfigurationUrl());
//            model.setVariables(prevModel.getVariables());

            // and the two "real" ones we look up each time to make sure they are current
            RemoteTerraformOutputsProcess outputCmd = RemoteTerraformOutputsProcess.of(this);
            outputCmd.run();
            model.setOutputsStringified(outputCmd.getOutputAsJsonStringized());
            model.setOutputs(outputCmd.getOutputAsMap());
            
            return statusSuccess();
        }

    }

}

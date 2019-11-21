package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.commands.TerraformOutputsCommand;
import software.amazon.cloudformation.proxy.ProgressEvent;

public class ReadHandler extends TerraformBaseHandler<ReadHandler.NoSteps> {

    protected enum NoSteps {}
    
    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> runStep() throws IOException {
        TerraformOutputsCommand outputCmd = TerraformOutputsCommand.of(this);
        outputCmd.run();
        model.setOutputsStringified(outputCmd.getOutputAsJsonStringized());
        model.setOutputs(outputCmd.getOutputAsMap());
    
        return progressEvents().success();
    }

}

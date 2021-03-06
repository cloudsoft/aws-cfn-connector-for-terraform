package io.cloudsoft.terraform.infrastructure.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudsoft.terraform.infrastructure.TerraformBaseWorker;
import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import io.cloudsoft.terraform.infrastructure.commands.SshToolbox.PostRunBehaviour;
import software.amazon.cloudformation.proxy.Logger;

import java.io.IOException;
import java.util.Map;

public class RemoteTerraformOutputsProcess extends RemoteTerraformProcess {

    private final ObjectMapper objectMapper;
    private String outputJsonStringized = null;

    public static RemoteTerraformOutputsProcess of(TerraformBaseWorker<?> w) {
        return new RemoteTerraformOutputsProcess(w.getParameters(), w.getLogger(), w.getModel().getIdentifier(), w.getCallbackContext().getCommandRequestId());
    }

    protected RemoteTerraformOutputsProcess(TerraformParameters params, Logger logger, String modelIdentifier, String commandIdentifier) {
        super(params, logger, modelIdentifier, commandIdentifier);
        this.objectMapper = new ObjectMapper();
    }

    public void run() throws IOException {
        ssh.runSSHCommand(String.format("cd %s && terraform output -json", getWorkDir()), PostRunBehaviour.FAIL, PostRunBehaviour.FAIL);
        outputJsonStringized = ssh.lastStdout;
        logger.log("Outputs from TF: '" + outputJsonStringized + "'");
        if (outputJsonStringized == null || outputJsonStringized.isEmpty()) {
            outputJsonStringized = "{}";
        }
    }

    public Map<String, Object> getOutputAsMap() throws IOException {
        return objectMapper.readValue(outputJsonStringized, new TypeReference<Map<String, Object>>() {
        });
    }

    public String getOutputAsJsonStringized() {
        return outputJsonStringized;
    }

}

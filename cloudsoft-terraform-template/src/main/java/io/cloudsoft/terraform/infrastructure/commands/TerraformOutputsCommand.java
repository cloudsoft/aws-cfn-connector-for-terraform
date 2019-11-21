package io.cloudsoft.terraform.infrastructure.commands;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudsoft.terraform.infrastructure.TerraformBaseHandler;
import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

public class TerraformOutputsCommand extends TerraformSshCommands {

    private final ObjectMapper objectMapper;
    private String outputJsonStringized = null;

    public static TerraformOutputsCommand of(TerraformBaseHandler<?> w) {
        return new TerraformOutputsCommand(w.getParameters(), w.logger, w.proxy, w.model.getIdentifier());
    }
    
    protected TerraformOutputsCommand(TerraformParameters params, Logger logger, AmazonWebServicesClientProxy proxy, String configurationIdentifier) {
        super(params, logger, proxy, configurationIdentifier);
        this.objectMapper = new ObjectMapper();
    }

    public void run() throws IOException {
        runSSHCommand(String.format("cd %s && terraform output -json", getWorkdir()));
        outputJsonStringized = getLastStdout();
        logger.log("Outputs from TF: '" + outputJsonStringized + "'");
        if (outputJsonStringized == null || outputJsonStringized.isEmpty()) {
            outputJsonStringized = "{}";
        }
    }

    public Map<String, Object> getOutputAsMap() throws IOException {
        return objectMapper.readValue(outputJsonStringized, new TypeReference<Map<String, Object>>() {
        });
    }

    public String getOutputAsJsonStringized() throws IOException {
        return outputJsonStringized;
    }

}

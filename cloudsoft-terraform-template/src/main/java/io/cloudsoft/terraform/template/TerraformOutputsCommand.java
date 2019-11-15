package io.cloudsoft.terraform.template;

import java.io.IOException;
import java.util.Map;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TerraformOutputsCommand extends TerraformInterfaceSSH {

    private final ObjectMapper objectMapper;

    public TerraformOutputsCommand(TerraformBaseHandler<?> h, Logger logger, AmazonWebServicesClientProxy proxy, String templateName) {
        super(h, logger, proxy, templateName);
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> run(Logger logger) throws IOException {
        runSSHCommand(String.format("cd ~/tfdata/'%s' && terraform output -json", templateName));
        String outputJsonStringized = getLastStdout();
        logger.log("Outputs from TF: '"+outputJsonStringized+"'");
        if (outputJsonStringized==null || outputJsonStringized.isEmpty()) {
            outputJsonStringized = "{}";
        }
        return objectMapper.readValue(outputJsonStringized, new TypeReference<Map<String, Object>>() {});
    }
}

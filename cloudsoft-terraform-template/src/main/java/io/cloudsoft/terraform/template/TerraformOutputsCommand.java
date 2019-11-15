package io.cloudsoft.terraform.template;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class TerraformOutputsCommand extends TerraformInterfaceSSH {

    private final ObjectMapper objectMapper;

    public TerraformOutputsCommand(TerraformBaseHandler<?> h, AmazonWebServicesClientProxy proxy, String templateName) {
        super(h, proxy, templateName);
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> run() throws IOException {
        Map<String, Object> output = null;
        runSSHCommand("terraform output -json");
        return objectMapper.readValue(getLastStdout(), new TypeReference<Map<String, Object>>() {});
    }
}

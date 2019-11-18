package io.cloudsoft.terraform.template;

import java.io.IOException;
import java.util.Map;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudsoft.terraform.template.worker.AbstractHandlerWorker;

public class TerraformOutputsCommand extends TerraformInterfaceSSH {

    private final ObjectMapper objectMapper;
    private String outputJsonStringized = null;
    
    protected TerraformOutputsCommand(TerraformBaseHandler<?> h, Logger logger, AmazonWebServicesClientProxy proxy, String templateName) {
        super(h, logger, proxy, templateName);
        this.objectMapper = new ObjectMapper();
    }

    public void run() throws IOException {
        runSSHCommand(String.format("cd '%s' && terraform output -json", getWorkdir()));
        outputJsonStringized = getLastStdout();
        logger.log("Outputs from TF: '"+outputJsonStringized+"'");
        if (outputJsonStringized==null || outputJsonStringized.isEmpty()) {
            outputJsonStringized = "{}";
        }
    }
    
    public Map<String, Object> getOutputAsMap() throws IOException {
        return objectMapper.readValue(outputJsonStringized, new TypeReference<Map<String, Object>>() {});
    }

    public String getOutputAsJsonStringized() throws IOException {
        return outputJsonStringized;
    }

    public static TerraformOutputsCommand of(AbstractHandlerWorker w) {
        return new TerraformOutputsCommand(w.handler, w.logger, w.proxy, w.model.getIdentifier());
    }
}

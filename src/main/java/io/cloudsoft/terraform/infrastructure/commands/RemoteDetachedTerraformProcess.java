package io.cloudsoft.terraform.infrastructure.commands;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import software.amazon.cloudformation.proxy.Logger;

abstract public class RemoteDetachedTerraformProcess extends RemoteTerraformProcess {
    
    protected String stdoutLogFileName, stderrLogFileName;
    final protected TerraformCommand tfCommand;

    public enum TerraformCommand {
        TF_INIT,
        TF_APPLY,
        TF_DESTROY,
    }

    protected RemoteDetachedTerraformProcess(TerraformParameters params, Logger logger, TerraformCommand tc, String modelIdentifier, String commandIdentifier) {
        super(params, logger, modelIdentifier, commandIdentifier);
        tfCommand = tc;
    }

    public String getCommandName() {
        return tfCommand.toString();
    }
    
    public String getFullStdout() throws IOException {
        return ssh.catFileIfExists(stdoutLogFileName);
    }

    public String getFullStderr() throws IOException {
        return ssh.catFileIfExists(stderrLogFileName);
    }

    public String getIncrementalStdout() throws IOException {
        return ssh.catIncrementalFileIfExists(stdoutLogFileName);
    }

    public String getIncrementalStderr() throws IOException {
        return ssh.catIncrementalFileIfExists(stderrLogFileName);
    }

    protected String getTerraformCommand() {
        switch (tfCommand) {
            case TF_INIT:
                return "terraform init -lock=true -no-color -input=false";
            case TF_APPLY:
                return "terraform apply -lock=true -no-color -input=false -auto-approve";
            case TF_DESTROY:
                return "terraform destroy -lock=true -no-color -auto-approve";
            default:
                throw new IllegalArgumentException("Unknown command " + tfCommand.toString());
        }
    }

    abstract public void start() throws IOException;
    abstract public boolean isRunning() throws IOException;
    abstract public boolean wasFailure() throws IOException;
    abstract public String getErrorString() throws IOException;
    abstract public void cleanup() throws IOException;
    
}

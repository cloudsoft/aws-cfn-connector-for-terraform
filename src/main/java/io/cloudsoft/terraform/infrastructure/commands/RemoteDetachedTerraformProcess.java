package io.cloudsoft.terraform.infrastructure.commands;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import software.amazon.cloudformation.proxy.Logger;

abstract public class RemoteDetachedTerraformProcess extends RemoteTerraformProcess {
    
    protected String stdoutLogFileName, stderrLogFileName;
    final protected TerraformCommand tfCommand;

    public enum TerraformCommand {
        TC_INIT,
        TC_APPLY,
        TC_DESTROY,
    }

    protected RemoteDetachedTerraformProcess(TerraformParameters params, Logger logger, TerraformCommand tc, String configurationIdentifier) {
        super(params, logger, configurationIdentifier);
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

    abstract public void start() throws IOException;
    abstract public boolean isRunning() throws IOException;
    abstract public boolean wasFailure() throws IOException;
    abstract public String getErrorString() throws IOException;
}

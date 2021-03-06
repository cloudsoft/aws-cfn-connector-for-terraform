package io.cloudsoft.terraform.infrastructure.commands;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.cloudsoft.terraform.infrastructure.TerraformBaseWorker;
import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import io.cloudsoft.terraform.infrastructure.commands.SshToolbox.PostRunBehaviour;
import software.amazon.cloudformation.proxy.Logger;

public class RemoteDetachedTerraformProcessSystemd extends RemoteDetachedTerraformProcess {

    public static RemoteDetachedTerraformProcessSystemd of(TerraformBaseWorker<?> w, TerraformCommand tc) {
        return new RemoteDetachedTerraformProcessSystemd(w.getParameters(), w.getLogger(), tc, w.getModel().getIdentifier(), w.getCallbackContext().getCommandRequestId());
    }

    protected RemoteDetachedTerraformProcessSystemd(TerraformParameters params, Logger logger, TerraformCommand tc, String modelIdentifier, String commandIdentifier) {
        super(params, logger, tc, modelIdentifier, commandIdentifier);
        stdoutLogFileName = String.format("%s/%s-stdout.log", getLogDir(), getUnitPrefix());
        stderrLogFileName = String.format("%s/%s-stderr.log", getLogDir(), getUnitPrefix());
    }
    
    protected String getLogDir() {
        return getWorkDir();
    }
    
    private String getUnitPrefix() {
        return "terraform-"+modelIdentifier+"-"+commandIdentifier+"-"+getCommandName().toLowerCase();
    }

    private String getUnitFullName() {
        return getUnitPrefix()+".service";
    }

    private String getRemotePropertyValue(String propName) throws IOException {
        ssh.runSSHCommand(String.format("systemctl --user show --property %s %s | cut -d= -f2", 
            propName, getUnitFullName()), PostRunBehaviour.IGNORE, PostRunBehaviour.IGNORE);
        return ssh.lastStdout.replaceAll("\n", "");
    }

    public void start() throws IOException {
        final List<String> commands = Arrays.asList(
                ssh.setupIncrementalFileCommand(stdoutLogFileName),
                ssh.setupIncrementalFileCommand(stderrLogFileName),
                "loginctl enable-linger",
                String.format("systemd-run"
                    + " --unit="+getUnitPrefix()
                    + " --user"
                    + " --remain-after-exit"   // this is required, otherwise we don't get exit code
                    + " -p WorkingDirectory="+getWorkDir()
                    + " -p StandardOutput=file:"+stdoutLogFileName
                    + " -p StandardError=file:"+stderrLogFileName
                    // indirection through `env` solves potential issue where cmd wants absolue path
                    + " /usr/bin/env " + getTerraformCommand()
                    
                    // note: could use -t and redirects, but better if we don't need them, and -p seems to work!
//                    + " -t"
//                    + " < /dev/null > "+stdoutLogFileName+" 2> "+stderrLogFileName
                    
                    )
        );
        ssh.runSSHCommand(String.join("; ", commands), PostRunBehaviour.FAIL, 
            PostRunBehaviour.IGNORE /* prints the unit prefix */ );
    }

    private String getSubState() throws IOException {
        return getRemotePropertyValue("SubState");
    }

    public boolean isRunning() throws IOException {
        // this doesn't work for transients where we say "remain-after-exit"
//        return "active".equals(getActiveState());

        return "running".equals(getSubState());
    }

    private String getResult() throws IOException {
        return getRemotePropertyValue("Result");
    }

    public boolean wasFailure() throws IOException {
        return !"success".equals(getResult());
    }

    private String getMainExitCode() throws IOException {
        return getRemotePropertyValue("ExecMainCode");
    }

    public String getErrorString() throws IOException {
        return String.format("result %s (%s)", getResult(), getMainExitCode());
    }
    
    @Override
    public void cleanup() throws IOException {
        // stop every run from polluting the user systemctl history
        ssh.runSSHCommand("systemctl --user stop "+getUnitFullName(), 
            PostRunBehaviour.WARN, PostRunBehaviour.IGNORE);
    }

}

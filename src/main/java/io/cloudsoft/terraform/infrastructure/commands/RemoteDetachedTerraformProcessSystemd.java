package io.cloudsoft.terraform.infrastructure.commands;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import io.cloudsoft.terraform.infrastructure.TerraformBaseWorker;
import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import software.amazon.cloudformation.proxy.Logger;

public class RemoteDetachedTerraformProcessSystemd extends RemoteDetachedTerraformProcess {

    private final String unitName;
    // systemd unit instance name is available as "configurationIdentifier" in the parent class.

    public static RemoteDetachedTerraformProcessSystemd of(TerraformBaseWorker<?> w, TerraformCommand tc) {
        return new RemoteDetachedTerraformProcessSystemd(w.getParameters(), w.getLogger(), tc, w.getModel().getIdentifier(), w.getCallbackContext().getCommandRequestId());
    }

    protected RemoteDetachedTerraformProcessSystemd(TerraformParameters params, Logger logger, TerraformCommand tc, String modelIdentifier, String commandIdentifier) {
        super(params, logger, tc, modelIdentifier, commandIdentifier);
        switch (tc) {
            case TF_INIT:
                unitName = "terraform-init";
                break;
            case TF_APPLY:
                unitName = "terraform-apply";
                break;
            case TF_DESTROY:
                unitName = "terraform-destroy";
                break;
            default:
                throw new IllegalArgumentException ("Invalid value " + tc.toString());
        }
        // NB: The two values below must be consistent with what is in the systemd unit files.
        stdoutLogFileName = String.format("%s/%s@%s-stdout-live.log", getLogDir(), unitName, getInstanceName());
        stderrLogFileName = String.format("%s/%s@%s-stderr-live.log", getLogDir(), unitName, getInstanceName());
    }
    
    protected String getLogDir() {
        return getWorkDir();
    }
    
    private String getInstanceName() {
        return commandIdentifier;
    }

    private String getRemotePropertyValue(String propName) throws IOException {
        ssh.runSSHCommand(String.format("systemctl --user show --property %s %s@%s | cut -d= -f2", propName, unitName, getInstanceName()));
        return ssh.lastStdout.replaceAll("\n", "");
    }

    public void start() throws IOException {
        final List<String> commands = Arrays.asList(
                // systemd neither replaces nor appends the log file on a 2nd run of the same unit, it just
                // starts writing over the pre-existing contents (at least the version 237-3ubuntu10.33).
                "truncate --size=0 " + stdoutLogFileName,
                "truncate --size=0 " + stderrLogFileName,
                ssh.setupIncrementalFileCommand(stdoutLogFileName),
                ssh.setupIncrementalFileCommand(stderrLogFileName),
                "loginctl enable-linger",
                String.format("systemctl --user --working-directory=%s start %s@%s", getWorkDir(), unitName, getInstanceName())
        );
        ssh.runSSHCommand(String.join("; ", commands));
    }

    private String getActiveState() throws IOException {
        return getRemotePropertyValue("ActiveState");
    }

    public boolean isRunning() throws IOException {
        return "active".equals(getActiveState());
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
}

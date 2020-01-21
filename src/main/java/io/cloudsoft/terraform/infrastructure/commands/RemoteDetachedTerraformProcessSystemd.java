package io.cloudsoft.terraform.infrastructure.commands;

import io.cloudsoft.terraform.infrastructure.TerraformBaseWorker;
import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import lombok.Getter;
import software.amazon.cloudformation.proxy.Logger;

import java.io.IOException;
import java.util.*;

public class RemoteDetachedTerraformProcessSystemd extends RemoteDetachedTerraformProcess {

    private final String unitName;
    // systemd unit instance name is available as "configurationIdentifier" in the parent class.

    public static RemoteDetachedTerraformProcessSystemd of(TerraformBaseWorker<?> w, TerraformCommand tc) {
        return new RemoteDetachedTerraformProcessSystemd(w.getParameters(), w.getLogger(), tc, w.getModel().getIdentifier());
    }

    protected RemoteDetachedTerraformProcessSystemd(TerraformParameters params, Logger logger, TerraformCommand tc, String configurationName) {
        super(params, logger, tc, configurationName);
        switch (tc) {
            case TC_INIT:
                unitName = "terraform-init";
                break;
            case TC_APPLY:
                unitName = "terraform-apply";
                break;
            case TC_DESTROY:
                unitName = "terraform-destroy";
                break;
            default:
                throw new IllegalArgumentException ("Invalid value " + tc.toString());
        }
        // NB: The two values below must be consistent with what is in the systemd unit files.
        stdoutLogFileName = String.format("%s/%s@%s-stdout-live.log", getWorkDir(), unitName, configurationIdentifier);
        stderrLogFileName = String.format("%s/%s@%s-stderr-live.log", getWorkDir(), unitName, configurationIdentifier);
    }

    private String getRemotePropertyValue(String propName) throws IOException {
        ssh.runSSHCommand(String.format("systemctl --user show --property %s %s@%s | cut -d= -f2", propName, unitName, configurationIdentifier));
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
                String.format("systemctl --user start %s@%s", unitName, configurationIdentifier)
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

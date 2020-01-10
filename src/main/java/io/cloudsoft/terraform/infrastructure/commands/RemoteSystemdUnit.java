package io.cloudsoft.terraform.infrastructure.commands;

import io.cloudsoft.terraform.infrastructure.TerraformBaseWorker;
import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import lombok.Getter;
import software.amazon.cloudformation.proxy.Logger;

import java.io.IOException;
import java.util.*;

public class RemoteSystemdUnit extends TerraformSshCommands {

    @Getter
    private final String unitName;
    private final String stdoutLogFileName, stderrLogFileName;

    public static RemoteSystemdUnit of(TerraformBaseWorker<?> w, String unitName) {
        return new RemoteSystemdUnit(w.getParameters(), w.getLogger(), unitName, w.getModel().getIdentifier());
    }

    protected RemoteSystemdUnit(TerraformParameters params, Logger logger, String unitName, String configurationName) {
        super(params, logger, configurationName);
        this.unitName = unitName + "@" + configurationName;
        // NB: The two values below must be consistent with what is in the systemd unit files.
        stdoutLogFileName = String.format("%s/%s-stdout-live.log", getWorkDir(), this.unitName);
        stderrLogFileName = String.format("%s/%s-stderr-live.log", getWorkDir(), this.unitName);
    }

    private String getRemotePropertyValue(String propName) throws IOException {
        runSSHCommand(String.format("systemctl --user show --property %s %s | cut -d= -f2", propName, unitName));
        return lastStdout.replaceAll("\n", "");
    }

    public void start() throws IOException {
        final List<String> commands = Arrays.asList(
                // systemd neither replaces nor appends the log file on a 2nd run of the same unit, it just
                // starts writing over the pre-existing contents (at least the version 237-3ubuntu10.33).
                "truncate --size=0 " + stdoutLogFileName,
                "truncate --size=0 " + stderrLogFileName,
                "loginctl enable-linger",
                "systemctl --user start " + unitName
        );
        runSSHCommand(String.join("; ", commands));
    }

    private String getActiveState() throws IOException {
        return getRemotePropertyValue("ActiveState");
    }

    public boolean isRunning() throws IOException {
        return "active".equals(getActiveState());
    }

    public String getResult() throws IOException {
        return getRemotePropertyValue("Result");
    }

    public boolean wasFailure() throws IOException {
        return !"success".equals(getResult());
    }

    public String getMainExitCode() throws IOException {
        return getRemotePropertyValue("ExecMainCode");
    }

    public String getFullStdout() throws IOException {
        return catFileIfExists(stdoutLogFileName);
    }

    public String getFullStderr() throws IOException {
        return catFileIfExists(stderrLogFileName);
    }

}

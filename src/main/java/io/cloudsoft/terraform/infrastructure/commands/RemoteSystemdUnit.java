package io.cloudsoft.terraform.infrastructure.commands;

import io.cloudsoft.terraform.infrastructure.TerraformBaseWorker;
import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import lombok.Getter;
import software.amazon.cloudformation.proxy.Logger;

import java.io.IOException;

public class RemoteSystemdUnit extends TerraformSshCommands {

    @Getter
    private final String unitName;

    public static RemoteSystemdUnit of(TerraformBaseWorker<?> w, String unitName) {
        return new RemoteSystemdUnit(w.getParameters(), w.getLogger(), unitName, w.getModel().getIdentifier());
    }

    protected RemoteSystemdUnit(TerraformParameters params, Logger logger, String unitName, String configurationName) {
        super(params, logger, configurationName);
        this.unitName = unitName + "@" + configurationName;
    }

    private String getRemotePropertyValue(String propName) throws IOException {
        runSSHCommand(String.format("systemctl --user show --property %s %s | cut -d= -f2", propName, unitName));
        return lastStdout.replaceAll("\n", "");
    }

    public void start() throws IOException {
        runSSHCommand(String.format("loginctl enable-linger; systemctl --user start %s", unitName));
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

    public String getLog() throws IOException {
        runSSHCommand(String.format("journalctl --no-pager --user-unit=%s", unitName));
        return getLastStdout() + "\n" + getLastStderr();
    }

}

package io.cloudsoft.terraform.infrastructure;

import io.cloudsoft.terraform.infrastructure.worker.AbstractHandlerWorker;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

import java.io.IOException;

public class RemoteSystemdUnit extends TerraformSshCommands {
    private String unitName;

    protected RemoteSystemdUnit(TerraformBaseHandler<?> h, Logger logger, AmazonWebServicesClientProxy proxy, String unitName, String configurationName) {
        super(h, logger, proxy, configurationName);
        this.unitName = String.format("%s@%s", unitName, configurationName);
    }

    private String getRemotePropertyValue(String propName) throws IOException {
        runSSHCommand(String.format("systemctl --user show --property %s %s | cut -d= -f2", propName, unitName));
        return lastStdout.replaceAll("\n", "");
    }

    public void start() throws IOException {
        runSSHCommand(String.format("systemctl --user start %s", unitName));
    }

    public String getActiveState() throws IOException {
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

    public String getErrno() throws IOException {
        return getRemotePropertyValue("StatusErrno");
    }

    public static RemoteSystemdUnit of(AbstractHandlerWorker w, String unitName) {
        return new RemoteSystemdUnit(w.handler, w.logger, w.proxy, unitName, w.model.getIdentifier());
    }

    public String getLog() throws IOException {
        runSSHCommand(String.format("journalctl --no-pager --user-unit=%s", unitName));
        return getLastStdout() + "\n" + getLastStderr();
    }

}

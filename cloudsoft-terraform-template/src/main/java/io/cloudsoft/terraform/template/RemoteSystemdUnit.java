package io.cloudsoft.terraform.template;

import java.io.IOException;

class RemoteSystemdUnit extends TerraformInterfaceSSH {
    private String unitName;

    RemoteSystemdUnit(TerraformBaseHandler<?> h, String unitName, String configurationName) {
        super(h, configurationName);
        this.unitName = String.format("%s@%s", unitName, configurationName);
    }

    private String getRemotePropertyValue(String propName) throws IOException {
        runSSHCommand(String.format("systemctl --user show --property %s %s | cut -d= -f2", propName, unitName));
        return lastStdout.replaceAll("\n", "");
    }

    void start() throws IOException {
        runSSHCommand(String.format("systemctl --user start %s", unitName));
    }

    boolean isRunning() throws IOException {
        return getRemotePropertyValue("ActiveState").equals("active");
    }

    boolean wasSuccess() throws IOException {
        return getRemotePropertyValue("Result").equals("success");
    }

    String getErrno() throws IOException {
        return getRemotePropertyValue("StatusErrno");
    }
}
package io.cloudsoft.terraform.template;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;

import java.io.IOException;

public class RemoteSystemdUnit extends TerraformInterfaceSSH {
    private String unitName;

    public RemoteSystemdUnit(TerraformBaseHandler<?> h, Logger logger, AmazonWebServicesClientProxy proxy, String unitName, String configurationName) {
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
    
}

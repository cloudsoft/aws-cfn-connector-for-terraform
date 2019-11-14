package io.cloudsoft.terraform.template;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TerraformInterfaceSSH {
    private final String templateName, serverHostname, sshUsername, sshServerKeyFP,
            sshClientSecretKeyContents;
    private final int sshPort;
    protected String lastStdout, lastStderr;
    protected int lastExitStatus;

    public TerraformInterfaceSSH(TerraformBaseHandler<?> h, AmazonWebServicesClientProxy proxy, String templateName) {
        this.serverHostname = h.getHost(proxy);
        this.sshPort = h.getPort(proxy);
        this.sshServerKeyFP = h.getFingerprint(proxy);
        this.sshUsername = h.getUsername(proxy);
        this.sshClientSecretKeyContents = h.getSSHKey(proxy);
        this.templateName = templateName;
    }

    public void onlyMkdir() throws IOException {
        runSSHCommand(String.format("mkdir -p ~/tfdata/'%s'", templateName));
    }

    public void onlyDownload(String url) throws IOException {
        runSSHCommand(String.format("cd ~/tfdata/'%s' && wget --output-document=configuration.tf '%s'", templateName, url));
    }

    public void onlyRmdir() throws IOException {
        runSSHCommand(String.format("rm -rf ~/tfdata/'%s'", templateName));
    }

    protected void runSSHCommand(String command) throws IOException {
        System.out.println("DEBUG: @" + serverHostname + "> " + command);

        final SSHClient ssh = new SSHClient();

        ssh.addHostKeyVerifier(sshServerKeyFP);
        ssh.connect(serverHostname, sshPort);
        Session session = null;
        try {
            ssh.authPublickey(sshUsername, new SSHClient().loadKeys(sshClientSecretKeyContents, null, null));
            session = ssh.startSession();
            final Session.Command cmd = session.exec(command);
            lastStdout = IOUtils.readFully(cmd.getInputStream()).toString();
            cmd.join(5, TimeUnit.SECONDS);
            lastExitStatus = cmd.getExitStatus();
            lastStderr = IOUtils.readFully(cmd.getErrorStream()).toString();
            System.out.println("stdout: " + lastStdout);
            System.out.println("stderr: " + lastStderr);
            System.out.println("exit status: " + lastExitStatus);
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (IOException e) {
                // do nothing
            }
            ssh.disconnect();
        }
    }
}

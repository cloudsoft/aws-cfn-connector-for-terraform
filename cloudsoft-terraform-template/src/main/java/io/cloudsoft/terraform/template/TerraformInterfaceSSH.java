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
    // Convert these constants to parameters later if necessary (more likely to be
    // useful after parameters can be specified separately for each server).
    private static final String
            // TF_DATADIR must match the contents of the files in server-side-systemd/
            TF_DATADIR = "~/tfdata",
            TF_CONFFILENAME = "configuration.tf";

    public TerraformInterfaceSSH(TerraformBaseHandler<?> h, AmazonWebServicesClientProxy proxy, String templateName) {
        this.serverHostname = h.getHost(proxy);
        this.sshPort = h.getPort(proxy);
        this.sshServerKeyFP = h.getFingerprint(proxy);
        this.sshUsername = h.getUsername(proxy);
        this.sshClientSecretKeyContents = h.getSSHKey(proxy);
        this.templateName = templateName;
    }

    protected String getWorkdir() {
        return String.format("%s/'%s'", TF_DATADIR, templateName);
    }

    public void onlyMkdir() throws IOException {
        runSSHCommand("mkdir -p " + getWorkdir());
    }

    public void onlyDownload(String url) throws IOException {
        runSSHCommand(String.format("cd %s && wget --output-document='%s' '%s'", getWorkdir(), TF_CONFFILENAME, url));
    }

    public void onlyRmdir() throws IOException {
        runSSHCommand("rm -rf " + getWorkdir());
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

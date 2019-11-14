package io.cloudsoft.terraform.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

public class TerraformInterfaceSSH {
    private final String templateName, serverHostname, sshUsername, sshServerKeyFP, 
        sshClientSecretKeyContents;
    private final int sshPort;
    protected String lastStdout, lastStderr;
    protected int lastExitStatus;

    public TerraformInterfaceSSH(TerraformBaseHandler<?> h, String templateName) {
        this.serverHostname = h.getHost();
        this.sshPort = h.getPort();
        this.sshServerKeyFP = h.getFingerprint();
        this.sshUsername = h.getUsername();        
        this.sshClientSecretKeyContents = h.getSSHKey();
        this.templateName = templateName;
    }
    
    public void onlyMkdir() throws IOException
    {
        runSSHCommand(String.format ("mkdir -p ~/tfdata/'%s'", templateName));
    }
    public void onlyDownload (String url) throws IOException
    {
        runSSHCommand(String.format ("cd ~/tfdata/'%s' && wget --output-document=configuration.tf '%s'", templateName, url));
    }

    void updateTemplateFromURL(String url) throws IOException {
        runSSHCommand(String.format ("cd ~/tfdata/'%s' && wget --output-document=configuration.tf '%s'", templateName, url));
        runSSHCommand(String.format ("cd ~/tfdata/'%s' && terraform apply -lock=true -input=false -auto-approve -no-color", templateName));
    }

    void updateTemplateFromContents(String contents) throws IOException {
        // TODO
    }

    void deleteTemplate() throws IOException {
        runSSHCommand(String.format ("cd ~/tfdata/'%s' && terraform destroy -lock=true -auto-approve -no-color", templateName));
        runSSHCommand(String.format ("rm -rf ~/tfdata/'%s'", templateName));
    }

    protected void runSSHCommand(String command) throws IOException {
        System.out.println("DEBUG: @" + serverHostname + "> " + command);

        final SSHClient ssh = new SSHClient();

        ssh.addHostKeyVerifier(sshServerKeyFP);
        ssh.connect(serverHostname, sshPort);
        Session session = null;
        try {
            ssh.authPublickey(sshUsername, getKeyProvider());
            session = ssh.startSession();
            final Session.Command cmd = session.exec(command);
            lastStdout = IOUtils.readFully(cmd.getInputStream()).toString();
            cmd.join(5, TimeUnit.SECONDS);
            lastExitStatus = cmd.getExitStatus();
            lastStderr = IOUtils.readFully(cmd.getErrorStream()).toString();
            //model.setMetricValue(s1); // does not compile
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
    
    private Iterable<KeyProvider> getKeyProvider() throws IOException {
        List<KeyProvider> result = new ArrayList<KeyProvider>();
        if (sshClientSecretKeyContents!=null && !sshClientSecretKeyContents.isEmpty()) {
            // add key provider from contents
            result.add(new SSHClient().loadKeys(
                sshClientSecretKeyContents,
                // TODO does this work, passing null for pub key? it looks like it should.
                null, 
                null));
        }
        return result;
    }
}

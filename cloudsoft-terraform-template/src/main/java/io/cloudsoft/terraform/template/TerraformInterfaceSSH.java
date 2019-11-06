package io.cloudsoft.terraform.template;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

class TerraformInterfaceSSH {
    private final String templateName, serverHostname, sshUsername, sshServerKeyFP, 
        sshClientSecretKeyContents, sshClientSecretKeyFile;

    TerraformInterfaceSSH(TerraformBaseHandler<?> h, String templateName) {
        this.serverHostname = h.getHost();
        // TODO port
        this.sshServerKeyFP = h.getFingerprint();
        this.sshUsername = h.getUsername();        
        this.sshClientSecretKeyContents = h.getSSHKey();
        this.sshClientSecretKeyFile = null;
        this.templateName = templateName;
    }
    
    TerraformInterfaceSSH(String serverHostname, String templateName) {
        // FIXME: fetch parameters from a state
        switch (serverHostname)
        {
            case "localhost":
                // In a Docker container loopback is in its own namespace, the client has to reach the host.
                this.serverHostname = "192.168.101.123";
                this.sshServerKeyFP = "3a:87:1f:a6:d8:6a:32:b7:47:fe:2d:e1:16:3a:bc:38";
                this.sshUsername = "denis";
                // The key file ends up at the root level of the JAR, hence pay attention to the path
                // to keep the code working in a lambda environment (in particular, SAM local tests
                // depend on this).
                this.sshClientSecretKeyFile = "id_rsa_java";
                break;
            case "tf-denis":
                this.serverHostname = "ec2-54-93-230-94.eu-central-1.compute.amazonaws.com";
                this.sshServerKeyFP = "bb:4e:e3:73:76:a0:4e:bc:58:7a:d5:6c:0d:a8:f8:12";
                this.sshUsername = "ubuntu";
                // Idem.
                this.sshClientSecretKeyFile = "terraform-denis-20191104.pem";
                break;
            default:
                throw new IllegalArgumentException ("Unknown Terraform server name '" + serverHostname + "'");
        }
        sshClientSecretKeyContents = null;
        this.templateName = templateName;
    }

    void createTemplateFromURL(String url) throws IOException {
        runSSHCommand("wget " + url);
        runSSHCommand("terraform init");
        runSSHCommand("terraform apply " + templateName);
    }

    void updateTemplateFromURL(String url) throws IOException {
        runSSHCommand("wget " + url);
        runSSHCommand("terraform apply " + templateName);
    }

    void createTemplateFromContents(String contents) throws IOException {
        // TODO
    }
    
    void updateTemplateFromContents(String contents) throws IOException {
        // TODO
    }

    void deleteTemplate() throws IOException {
        runSSHCommand("terraform destroy " + templateName);
    }

    private void runSSHCommand(String command) throws IOException {
        command = "echo " + command;
        // System.out.println("DEBUG: " + serverHostname + " ¬ " + command);

        final SSHClient ssh = new SSHClient();

        // FIXME: keep the fingerprint(s) in an external state instead of hardcoding
        // loadKnownHosts() has no effect even when run on the dev PC
        ssh.addHostKeyVerifier(sshServerKeyFP);

        ssh.connect(serverHostname);
        Session session = null;
        try {

            ssh.authPublickey(sshUsername, getKeyProvider());

            session = ssh.startSession();
            final Session.Command cmd = session.exec(command);
            String s1 = IOUtils.readFully(cmd.getInputStream()).toString();
            cmd.join(5, TimeUnit.SECONDS);
            String s2 = "exit status: " + cmd.getExitStatus(); // TBD
            // cmd.getErrorStream() // TBD
            //model.setMetricValue(s1); // does not compile
            System.out.println(s1);
            System.out.println(s2);
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
        if (sshClientSecretKeyFile!=null && !sshClientSecretKeyFile.isEmpty()) {
            // src/main/resources/privkey works.
            // /home/user/.ssh/privkey works if the file has no passphrase (sshj does
            // not support SSH agent, and there is no SSH agent in AWS anyway).
            // ~/.ssh/privkey does not work.
            // ~user/.ssh/privkey does not work.
            result.add(new SSHClient().loadKeys(sshClientSecretKeyFile));
        }
        return result;
    }
}
package io.cloudsoft.terraform.infrastructure.commands;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import software.amazon.cloudformation.proxy.Logger;

public class SshToolbox {

    protected final Logger logger;
    protected final String serverHostname, sshUsername, sshServerKeyFP,
            sshClientSecretKeyContents;
    protected final int sshPort;
    protected String lastStdout, lastStderr;
    protected Integer lastExitStatusOrNull;

    protected SshToolbox(TerraformParameters params, Logger logger) {
        this.logger = logger;
        this.serverHostname = params.getHost();
        this.sshPort = params.getPort();
        this.sshServerKeyFP = params.getFingerprint();
        this.sshUsername = params.getUsername();
        this.sshClientSecretKeyContents = params.getSSHKey();
    }

    protected void mkdir(String dir) throws IOException {
        runSSHCommand("mkdir -p " + dir);
    }

    protected void rmdir(String dir) throws IOException {
        runSSHCommand("rm -rf " + dir);
    }

    protected void debug(String message) {
        // generates a lot of output, but can be useful
        // sysout makes it appear in SAM tests but not cloudwatch,
        // which is a good compromise in most cases
        System.out.println(message);
//        logger.log(message);
    }

    protected void runSSHCommand(String command) throws IOException {
        debug("DEBUG: @" + serverHostname + "> " + command);

        final SSHClient ssh = new SSHClient();

        addHostKeyVerifier(ssh);
        ssh.connect(serverHostname, sshPort);
        Session session = null;
        try {
            ssh.authPublickey(sshUsername, ssh.loadKeys(sshClientSecretKeyContents, null, null));
            session = ssh.startSession();
            final Session.Command cmd = session.exec(command);
            cmd.join(30, TimeUnit.SECONDS);
            lastExitStatusOrNull = cmd.getExitStatus();
            lastStdout = IOUtils.readFully(cmd.getInputStream()).toString();
            lastStderr = IOUtils.readFully(cmd.getErrorStream()).toString();
            debug("stdout: " + lastStdout);
            debug("stderr: " + lastStderr);
            debug("exit status: " + lastExitStatusOrNull);
            if (!((Integer) 0).equals(lastExitStatusOrNull) || !lastStderr.isEmpty()) {
                logger.log("Unexpected result/output from command '" + command + "': " + lastExitStatusOrNull + "\n"
                        + "  stderr: " + lastStderr + "\n"
                        + "  stdout: " + lastStdout);
            }
        } finally {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (IOException e) {
                // do nothing
            }
            try {
                ssh.disconnect();
                ssh.close();
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    protected void uploadFile(String dirName, String fileName, byte[] contents) throws IOException {
        final BytesSourceFile src = new BytesSourceFile(fileName, contents);
        final SSHClient ssh = new SSHClient();
        addHostKeyVerifier(ssh);
        ssh.connect(serverHostname, sshPort);
        try {
            ssh.authPublickey(sshUsername, ssh.loadKeys(sshClientSecretKeyContents, null, null));
            ssh.newSCPFileTransfer().upload(src, dirName);
        } finally {
            try {
                ssh.disconnect();
                ssh.close();
            } catch (Exception ee) {
                // ignore
            }
        }
    }

    protected String catFileIfExists(String remotePath) throws IOException {
        runSSHCommand(String.format("[ -f %s ] && cat %s || :", remotePath, remotePath));
        return lastStdout;
    }

    private String getSnapshotFileName(String fn) {
        return fn + ".snapshot";
    }

    private String getOffsetFileName(String fn) {
        return fn + ".offset";
    }

    protected String setupIncrementalFileCommand(String fn) throws IOException {
        return String.format("truncate --size=0 %s; echo 0 > %s", getSnapshotFileName(fn), getOffsetFileName(fn));
    }

    protected String catIncrementalFileIfExists(String fn) throws IOException {
        final String sfn = getSnapshotFileName(fn), ofn = getOffsetFileName(fn);
        runSSHCommand(String.format("cp %s %s; dd status=none if=%s bs=1 skip=`cat %s`; wc -c <%s >%s",
                fn, sfn, sfn, ofn, sfn, ofn));
        return lastStdout;
    }

    private void addHostKeyVerifier(SSHClient ssh) {
        if (sshServerKeyFP!=null && sshServerKeyFP.length()>0) {
            ssh.addHostKeyVerifier(sshServerKeyFP);
        } else {
            ssh.addHostKeyVerifier((host, port, key) -> true);
        }
    }

    private static class BytesSourceFile extends InMemorySourceFile {
        final private String name;
        final private byte[] contents;

        BytesSourceFile(String name, byte[] contents) {
            this.name = name;
            this.contents = contents;
        }

        public String getName() {
            return name;
        }

        public long getLength() {
            return contents.length;
        }

        public InputStream getInputStream() {
            return new ByteArrayInputStream(contents);
        }
    }

}

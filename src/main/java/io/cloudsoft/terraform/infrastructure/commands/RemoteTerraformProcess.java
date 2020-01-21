package io.cloudsoft.terraform.infrastructure.commands;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudsoft.terraform.infrastructure.TerraformBaseWorker;
import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import software.amazon.cloudformation.proxy.Logger;

public class RemoteTerraformProcess {
    
    // TF_DATADIR must match the contents of the files in server-side-systemd/
    // (at least as far as realpath(1) is concerned).
    // sshj does not expand tilde to the remote user's home directory on the server
    // (OpenSSH scp does that). Also neither any directory components nor the
    // file name can be quoted (as in "/some/'work dir'/otherdir") because sshj
    // fails to escape the quotes properly (again, works in OpenSSH).
    private static final String TF_DATADIR = "~/tfdata";

    protected final String configurationIdentifier;
    protected final SshToolbox ssh;
    protected final Logger logger;

    // Convert these constants to parameters later if necessary (more likely to be
    // useful after parameters can be specified separately for each server).
    private static final String
            TF_SCPDIR = "/tmp",
            TF_TMPFILENAME = "configuration.bin",
            TF_CONFFILENAME = "configuration.tf";

    public static RemoteTerraformProcess of(TerraformBaseWorker<?> w) {
        return new RemoteTerraformProcess(w.getParameters(), w.getLogger(), w.getModel().getIdentifier());
    }

    protected RemoteTerraformProcess(TerraformParameters params, Logger logger, String configurationIdentifier) {
        this.logger = logger;
        ssh = new SshToolbox(params, logger);
        this.configurationIdentifier = configurationIdentifier;
    }

    protected String getWorkDir() {
        return TF_DATADIR + "/" + configurationIdentifier;
    }

    public void mkWorkDir() throws IOException {
        ssh.mkdir(getWorkDir());
    }

    public void rmWorkDir() throws IOException {
        ssh.rmdir(getWorkDir());
    }
    
    private String getScpDir() {
        return TF_SCPDIR + "/" + configurationIdentifier;
    }

    public void uploadConfiguration(byte[] contents, Map<String, Object> vars_map) throws IOException, IllegalArgumentException {
        ssh.mkdir(getScpDir());
        ssh.uploadFile(getScpDir(), TF_TMPFILENAME, contents);
        final String tmpFilename = getScpDir() + "/" + TF_TMPFILENAME;
        ssh.runSSHCommand("file  --brief --mime-type " + tmpFilename);
        final String mimeType = ssh.lastStdout.replaceAll("\n", "");

        switch (mimeType) {
            case "text/plain":
                ssh.runSSHCommand(String.format("mv %s %s/%s", tmpFilename, getWorkDir(), TF_CONFFILENAME));
                break;
            case "application/zip":
                ssh.runSSHCommand(String.format("unzip %s -d %s", tmpFilename, getWorkDir()));
                break;
            default:
                ssh.rmdir(getScpDir());
                throw new IllegalArgumentException("Unknown MIME type " + mimeType);
        }
        if (vars_map != null && !vars_map.isEmpty()) {
            final String vars_filename = "cfn-" + configurationIdentifier + ".auto.tfvars.json";
            final byte[] vars_json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(vars_map);
            // Work around the tilde [non-]expansion as explained above.
            ssh.uploadFile(getScpDir(), vars_filename, vars_json);
            ssh.runSSHCommand(String.format("mv %s/%s %s/%s", getScpDir(), vars_filename, getWorkDir(), vars_filename));
        }
        ssh.rmdir(getScpDir());
    }

}

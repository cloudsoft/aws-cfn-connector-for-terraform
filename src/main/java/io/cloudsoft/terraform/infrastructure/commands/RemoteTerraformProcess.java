package io.cloudsoft.terraform.infrastructure.commands;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cloudsoft.terraform.infrastructure.TerraformBaseWorker;
import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import io.cloudsoft.terraform.infrastructure.commands.SshToolbox.PostRunBehaviour;
import software.amazon.cloudformation.proxy.Logger;

public class RemoteTerraformProcess {
    
    // Convert these constants to parameters later if necessary (more likely to be
    // useful after parameters can be specified separately for each server).
    
    // TF_DATADIR must match the contents of the files in server-side-systemd/
    // (at least as far as realpath(1) is concerned).
    // sshj does not expand tilde to the remote user's home directory on the server
    // (OpenSSH scp does that). Also neither any directory components nor the
    // file name can be quoted (as in "/some/'work dir'/otherdir") because sshj
    // fails to escape the quotes properly (again, works in OpenSSH).
    
    private static final String
        TF_DATADIR = "~/tfdata",
        TF_SCPDIR = "/tmp",
        TF_CFN_METADATA_JSON = "cfn-metadata.json",
        TF_CONFFILENAME = "configuration.tf";

    /** This should be the same for all runs against a particular TF deployment managed by CFN,
     * ie across all commands. */
    protected final String modelIdentifier;
    
    /** This is the same within a run, ie across steps,
     * but it is helpful if it is different between different commands. */
    protected final String commandIdentifier;
    
    protected final SshToolbox ssh;
    protected final Logger logger;

    public static RemoteTerraformProcess of(TerraformBaseWorker<?> w) {
        return new RemoteTerraformProcess(w.getParameters(), w.getLogger(), w.getModel().getIdentifier(), w.getCallbackContext().getCommandRequestId());
    }

    protected RemoteTerraformProcess(TerraformParameters params, Logger logger, String modelIdentifier, String commandIdentifier) {
        this.logger = logger;
        ssh = new SshToolbox(params, logger);
        this.modelIdentifier = modelIdentifier;
        this.commandIdentifier = commandIdentifier;
    }

    /** parent directory where all TF runs, for any stack/user/etc live */ 
    protected String getBaseDir() {
        return TF_DATADIR;
    }
    
    protected String getWorkDir() {
        return getBaseDir() + "/" + modelIdentifier;
    }

    public void mkWorkDir() throws IOException {
        ssh.mkdir(getWorkDir());
    }

    public void rmWorkDir() throws IOException {
        ssh.rmdir(getWorkDir());
    }
    
    private String getScpTmpDir() {
        return TF_SCPDIR + "/" + modelIdentifier;
    }

    public void uploadConfiguration(byte[] contents, Map<String, Object> vars_map, boolean firstTime) throws IOException, IllegalArgumentException {
        ssh.mkdir(getScpTmpDir());
        
        String tmpFileBasename = "terraform-upload-"+commandIdentifier+"-"+RandomStringUtils.randomAlphanumeric(4)+".file";
        ssh.uploadFile(getScpTmpDir(), tmpFileBasename, contents);
        final String tmpFilename = getScpTmpDir() + "/" + tmpFileBasename;
        ssh.runSSHCommand("file  --brief --mime-type " + tmpFilename, PostRunBehaviour.FAIL, PostRunBehaviour.IGNORE);
        final String mimeType = ssh.lastStdout.trim();
        switch (mimeType) {
            case "text/plain":
                ssh.runSSHCommand(String.format("mv %s %s/%s", tmpFilename, getWorkDir(), TF_CONFFILENAME), PostRunBehaviour.FAIL, PostRunBehaviour.IGNORE);
                break;
            case "application/zip":
                ssh.runSSHCommand(String.format("unzip %s -d %s", tmpFilename, getWorkDir()), PostRunBehaviour.FAIL, PostRunBehaviour.IGNORE);
                break;
            default:
                ssh.rmdir(getScpTmpDir());
                throw new IllegalArgumentException("Unknown MIME type " + mimeType);
        }
        
        final String vars_filename = "cfn-" + modelIdentifier + ".auto.tfvars.json";
        if (vars_map != null && !vars_map.isEmpty()) {
            final byte[] vars_json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(vars_map);
            // Work around the tilde [non-]expansion as explained above.
            ssh.uploadFile(getScpTmpDir(), vars_filename, vars_json);
            ssh.runSSHCommand(String.format("mv %s/%s %s/%s", getScpTmpDir(), vars_filename, getWorkDir(), vars_filename), PostRunBehaviour.FAIL, PostRunBehaviour.IGNORE);
        } else if (!firstTime) {
            // delete an old vars file if updating with no vars, in case there were vars there previously
            ssh.runSSHCommand(String.format("rm -f %s/%s", getWorkDir(), vars_filename), PostRunBehaviour.FAIL, PostRunBehaviour.IGNORE);
        }
        
        ssh.rmdir(getScpTmpDir());
    }

    // provide a way to store metadata on the server
    public void saveMetadata(Map<String,Object> metadata) throws IOException {
        ssh.mkdir(getScpTmpDir());
        String tmpFileBasename = "terraform-upload-metadata-"+RandomStringUtils.randomAlphanumeric(4)+".file";
        // Work around the tilde [non-]expansion as explained above.
        final byte[] vars_json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(metadata);
        ssh.uploadFile(getScpTmpDir(), tmpFileBasename, vars_json);
        ssh.mkdir(getWorkDir());
        ssh.runSSHCommand(String.format("mv %s/%s %s/%s", getScpTmpDir(), tmpFileBasename, 
            getWorkDir(), TF_CFN_METADATA_JSON), PostRunBehaviour.FAIL, PostRunBehaviour.IGNORE);
        ssh.rmdir(getScpTmpDir());
    }

    // provide a way to store metadata on the server
    public Map<?,?> loadMetadata() throws IOException {
        ssh.runSSHCommand("cat "+getWorkDir()+"/"+TF_CFN_METADATA_JSON, PostRunBehaviour.FAIL, PostRunBehaviour.FAIL);
        return new ObjectMapper().readValue(ssh.lastStdout.getBytes(), Map.class);
    }
    
}

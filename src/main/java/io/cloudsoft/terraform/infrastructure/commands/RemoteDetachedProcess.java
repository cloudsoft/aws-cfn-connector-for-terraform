package io.cloudsoft.terraform.infrastructure.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudsoft.terraform.infrastructure.TerraformBaseWorker;
import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import software.amazon.cloudformation.proxy.Logger;

import java.io.IOException;
import java.util.Map;

abstract public class RemoteDetachedProcess extends RemoteProcess {
    // Convert these constants to parameters later if necessary (more likely to be
    // useful after parameters can be specified separately for each server).
    private static final String
            TF_SCPDIR = "/tmp",
            TF_TMPFILENAME = "configuration.bin",
            TF_CONFFILENAME = "configuration.tf";
    protected String stdoutLogFileName, stderrLogFileName;
    final protected TerraformCommand tfCommand;

    public enum TerraformCommand {
        TC_INIT,
        TC_APPLY,
        TC_DESTROY,
    }


    public String getCommandName()
    {
        return tfCommand.toString();
    }

/*
    public static RemoteDetachedProcess of(TerraformBaseWorker<?> w, TerraformCommand tc) {
        return new RemoteDetachedProcess(w.getParameters(), w.getLogger(), tc, w.getModel().getIdentifier());
    }
*/

    protected RemoteDetachedProcess(TerraformParameters params, Logger logger, TerraformCommand tc, String configurationIdentifier) {
        super(params, logger, configurationIdentifier);
        tfCommand = tc;
    }

    private String getScpDir() {
        return TF_SCPDIR + "/" + configurationIdentifier;
    }

    public void uploadConfiguration(byte[] contents, Map<String, Object> vars_map) throws IOException, IllegalArgumentException {
        mkdir(getScpDir());
        uploadFile(getScpDir(), TF_TMPFILENAME, contents);
        final String tmpFilename = getScpDir() + "/" + TF_TMPFILENAME;
        runSSHCommand("file  --brief --mime-type " + tmpFilename);
        final String mimeType = lastStdout.replaceAll("\n", "");

        switch (mimeType) {
            case "text/plain":
                runSSHCommand(String.format("mv %s %s/%s", tmpFilename, getWorkDir(), TF_CONFFILENAME));
                break;
            case "application/zip":
                runSSHCommand(String.format("unzip %s -d %s", tmpFilename, getWorkDir()));
                break;
            default:
                rmdir(getScpDir());
                throw new IllegalArgumentException("Unknown MIME type " + mimeType);
        }
        if (vars_map != null && !vars_map.isEmpty()) {
            final String vars_filename = "cfn-" + configurationIdentifier + ".auto.tfvars.json";
            final byte[] vars_json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(vars_map);
            // Work around the tilde [non-]expansion as explained above.
            uploadFile(getScpDir(), vars_filename, vars_json);
            runSSHCommand(String.format("mv %s/%s %s/%s", getScpDir(), vars_filename, getWorkDir(), vars_filename));
        }
        rmdir(getScpDir());
    }

    public String getFullStdout() throws IOException {
        return catFileIfExists(stdoutLogFileName);
    }

    public String getFullStderr() throws IOException {
        return catFileIfExists(stderrLogFileName);
    }

    public String getIncrementalStdout() throws IOException {
        return catIncrementalFileIfExists(stdoutLogFileName);
    }

    public String getIncrementalStderr() throws IOException {
        return catIncrementalFileIfExists(stderrLogFileName);
    }

    abstract public void start() throws IOException;
    abstract public boolean isRunning() throws IOException;
    abstract public boolean wasFailure() throws IOException;
    abstract public String getErrorString() throws IOException;
}

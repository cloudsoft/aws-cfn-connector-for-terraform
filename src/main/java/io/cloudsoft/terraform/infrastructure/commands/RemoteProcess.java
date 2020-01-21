package io.cloudsoft.terraform.infrastructure.commands;

import io.cloudsoft.terraform.infrastructure.TerraformBaseWorker;
import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import software.amazon.cloudformation.proxy.Logger;

import java.io.IOException;

public class RemoteProcess extends TerraformSshCommands {
    // TF_DATADIR must match the contents of the files in server-side-systemd/
    // (at least as far as realpath(1) is concerned).
    // sshj does not expand tilde to the remote user's home directory on the server
    // (OpenSSH scp does that). Also neither any directory components nor the
    // file name can be quoted (as in "/some/'work dir'/otherdir") because sshj
    // fails to escape the quotes properly (again, works in OpenSSH).
    private static final String TF_DATADIR = "~/tfdata";

    protected final String configurationIdentifier;

    public static RemoteProcess of(TerraformBaseWorker<?> w) {
        return new RemoteProcess(w.getParameters(), w.getLogger(), w.getModel().getIdentifier());
    }

    protected RemoteProcess(TerraformParameters params, Logger logger, String configurationIdentifier) {
        super(params, logger);
        this.configurationIdentifier = configurationIdentifier;
    }

    protected String getWorkDir() {
        return TF_DATADIR + "/" + configurationIdentifier;
    }

    public void mkWorkDir() throws IOException {
        mkdir(getWorkDir());
    }

    public void rmWorkDir() throws IOException {
        rmdir(getWorkDir());
    }

}

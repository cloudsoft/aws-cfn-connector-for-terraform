package io.cloudsoft.terraform.infrastructure.commands;

import java.io.IOException;

import io.cloudsoft.terraform.infrastructure.TerraformBaseWorker;
import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import io.cloudsoft.terraform.infrastructure.commands.SshToolbox.PostRunBehaviour;
import software.amazon.cloudformation.proxy.Logger;

public class RemoteDetachedTerraformProcessNohup extends RemoteDetachedTerraformProcess {
    private final String pidFileName;
    protected final String exitstatusFileName;
    
    public static RemoteDetachedTerraformProcessNohup of(TerraformBaseWorker<?> w, TerraformCommand command) {
        return new RemoteDetachedTerraformProcessNohup(w.getParameters(), w.getLogger(), command, w.getModel().getIdentifier(), w.getCallbackContext().getCommandRequestId());
    }

    public RemoteDetachedTerraformProcessNohup(TerraformParameters parameters, Logger logger, TerraformCommand tc, String modelIdentifier, String commandIdentifier) {
        super(parameters, logger, tc, modelIdentifier, commandIdentifier);
        stdoutLogFileName = getFileName(true, "stdout.log");
        stderrLogFileName = getFileName(true, "stderr.log");
        exitstatusFileName = getFileName(true, "exitstatus.log");
        pidFileName = getFileName(true, "pid.txt");
    }
    
    private String getFileName(boolean isAbsolute, String trailer) {
        return (isAbsolute ? getWorkDir()+"/" : "") + 
            String.format("terraform-%s-%s-", commandIdentifier, getCommandName().toLowerCase()) +
            trailer;
    }

    public boolean wasFailure() { 
        String err = getErrorString();
        if (err==null || err.trim().length()==0) {
            // still running
            return false;
        }
        try {
            return Integer.parseInt(err.trim())!=0;
        } catch (Exception e) {
            throw new IllegalStateException("Unparseable error status: '"+err.trim()+"'");
        }
    }

    public String getErrorString() { 
        try {
            return ssh.catFileIfExists(exitstatusFileName); 
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isRunning() throws IOException {
        // Test a pathname that does not result in a false positive when the "cat"
        // sub-shell fails because e.g. the pidfile or the working directory does
        // not exist. In particular, "cmdline" would not be a good choice.
        ssh.runSSHCommand(String.format("[ -f /proc/`cat %s`/environ ] && echo true || echo false", pidFileName), PostRunBehaviour.IGNORE, PostRunBehaviour.IGNORE);
        String out = ssh.lastStdout.trim();
        if (out.equals("true")) {
            return true;
        } else if (out.equals("false")) {
            return false;
        } else {
            throw new IllegalStateException("Unexpected output from isRunning: '"+out+"'");
        }
    }

    public void start() throws IOException {
        String scriptName = "./"+getFileName(false, "script.sh");
        String fullCmd = String.join("\n", 
            "cd "+getWorkDir(),
            ssh.setupIncrementalFileCommand(stdoutLogFileName),
            ssh.setupIncrementalFileCommand(stderrLogFileName),
            "cat > "+scriptName+" << EOF",
            getTerraformCommand(),
            "echo \\$? > "+exitstatusFileName,
            "EOF",
            "chmod +x "+scriptName,
            String.format("nohup %s </dev/null >%s 2>%s & echo $! >%s", scriptName, stdoutLogFileName, stderrLogFileName, pidFileName)
            );
        ssh.runSSHCommand(fullCmd, PostRunBehaviour.FAIL, PostRunBehaviour.IGNORE);
    }
    
    @Override
    public void cleanup() {
    }

}

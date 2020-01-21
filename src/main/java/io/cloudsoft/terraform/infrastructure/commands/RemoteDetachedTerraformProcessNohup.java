package io.cloudsoft.terraform.infrastructure.commands;

import java.io.IOException;
import java.util.UUID;

import io.cloudsoft.terraform.infrastructure.TerraformBaseWorker;
import io.cloudsoft.terraform.infrastructure.TerraformParameters;
import software.amazon.cloudformation.proxy.Logger;

public class RemoteDetachedTerraformProcessNohup extends RemoteDetachedTerraformProcess {
    private final String pidFileName;
    protected final String exitstatusFileName;
    
    public static RemoteDetachedTerraformProcessNohup of(TerraformBaseWorker<?> w, TerraformCommand command) {
        return new RemoteDetachedTerraformProcessNohup(w.getParameters(), w.getLogger(), command, w.getModel().getIdentifier());
    }

    public RemoteDetachedTerraformProcessNohup(TerraformParameters parameters, Logger logger, TerraformCommand tc, String identifier) {
        super(parameters, logger, tc, identifier);
        stdoutLogFileName = String.format("%s/terraform-%s-%s-stdout.log", getWorkDir(), getCommandName(), configurationIdentifier);
        stderrLogFileName = String.format("%s/terraform-%s-%s-stderr.log", getWorkDir(), getCommandName(), configurationIdentifier);
        exitstatusFileName = String.format("%s/terraform-%s-%s-exitstatus.log", getWorkDir(), getCommandName(), configurationIdentifier);
        pidFileName = String.format("%s/terraform-%s-$s.pid", getWorkDir(), getCommandName(), configurationIdentifier);
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
        ssh.runSSHCommand(String.format("[ -d /proc/`cat %s` ] && echo true || echo false", pidFileName));
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
        final String tfCmd;
        switch (tfCommand) {
            case TC_INIT:
                tfCmd = "terraform init -lock=true -no-color -input=false";
                break;
            case TC_APPLY:
                tfCmd = "terraform apply -lock=true -no-color -input=false -auto-approve";
                break;
            case TC_DESTROY:
                tfCmd = "terraform destroy -lock=true -no-color -auto-approve";
                break;
            default:
                throw new IllegalArgumentException("Unknown command " + tfCommand.toString());
        }
        String scriptName = "terraform-"+getCommandName()+"-"+configurationIdentifier+".sh";
        String fullCmd = String.join("\n", 
            "cd "+getWorkDir(),
            ssh.setupIncrementalFileCommand(stdoutLogFileName),
            ssh.setupIncrementalFileCommand(stderrLogFileName),
            "cat > "+scriptName+" << EOF",
            tfCmd,
            "echo '$?' > "+exitstatusFileName,
            "EOF",
            "chmod +x "+scriptName,
            String.format("nohup %s </dev/null >%s 2>%s & echo $! >%s", scriptName, stdoutLogFileName, stderrLogFileName, pidFileName)
            );
        ssh.runSSHCommand(fullCmd);
    }
}

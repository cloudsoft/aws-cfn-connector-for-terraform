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
        stdoutLogFileName = String.format("%s/terraform-%s-%s-stdout.log", getWorkDir(), tc.toString(), configurationIdentifier);
        stderrLogFileName = String.format("%s/terraform-%s-%s-stderr.log", getWorkDir(), tc.toString(), configurationIdentifier);
        exitstatusFileName = String.format("%s/terraform-%s-%s-exitstatus.log", getWorkDir(), tc.toString(), configurationIdentifier);
        pidFileName = String.format("%s/%s@%s.pid", getWorkDir(), tc.toString(), configurationIdentifier);
    }

    public boolean wasFailure() { return false; }

    public String getErrorString() { return "unknown error"; }

    public boolean isRunning() throws IOException {
        ssh.runSSHCommand(String.format("[ -d /proc/`cat %s` ] && echo true || echo false", pidFileName));
        return ssh.lastStdout.equals("true");
    }

    public void start() throws IOException {
        final String cmd;
        switch (tfCommand) {
            case TC_INIT:
                cmd = "terraform init -lock=true -no-color -input=false";
                break;
            case TC_APPLY:
                cmd = "terraform apply -lock=true -no-color -input=false -auto-approve";
                break;
            case TC_DESTROY:
                cmd = "terraform destroy -lock=true -no-color -auto-approve";
                break;
            default:
                throw new IllegalArgumentException("Unknown command " + tfCommand.toString());
        }
        String scriptName = "terraform-command-"+UUID.randomUUID();
        String.join("\n", 
            "cat > "+scriptName+" << EOF",
            cmd,
            "echo $? > "+exitstatusFileName+"\n"
            + "EOF\n"
            +"chmod +x "+scriptName+"\n"
            + String.format("nohup %s </dev/null >%s 2>%s & echo $! >%s", cmd, stdoutLogFileName, stderrLogFileName, pidFileName)
            ;
        ssh.runSSHCommand(String.format("nohup %s </dev/null >%s 2>%s & echo $! >%s", cmd, stdoutLogFileName, stderrLogFileName, pidFileName));
    }
}

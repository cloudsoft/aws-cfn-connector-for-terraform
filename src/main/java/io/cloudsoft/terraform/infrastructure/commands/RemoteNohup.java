package io.cloudsoft.terraform.infrastructure.commands;

import java.io.IOException;

public class RemoteNohup extends RemoteDetachedProcess {
    private final String pidFileName;

    RemoteNohup (TerraformCommand tc)
    {
        stdoutLogFileName = String.format("%s/terraform-%s-%s-stdout.log", getWorkDir(), tc.toString(), configurationIdentifier);
        stderrLogFileName = String.format("%s/terraform-%s-%s-stderr.log", getWorkDir(), tc.toString(), configurationIdentifier);
        pidFileName = String.format("%s/%s@%s.pid", getWorkDir(), tc.toString(), configurationIdentifier);
    }

    public boolean wasFailure() { return false; }

    public String getErrorString() { return "unknown error"; }

    public boolean isRunning() throws IOException {
        runSSHCommand(String.format("[ -d /proc/`cat %s` ] && echo true || echo false", pidFileName));
        return lastStdout.equals("true");
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
        runSSHCommand(String.format("nohup %s </dev/null >%s 2>%s & echo $! >%s", cmd, stdoutLogFileName, stderrLogFileName, pidFileName));
    }
}

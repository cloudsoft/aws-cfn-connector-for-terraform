package io.cloudsoft.terraform.template;

public class AsyncSshHelper {

    CallbackContext callback;
    
    boolean running;
    String stdout;
    String stderr;
    
    public AsyncSshHelper(CallbackContext callback) {
        this.callback = callback;
    }
    
    protected String exec(String command) {
        // TODO run command over ssh, return the output
        return "TODO "+marker("PID")+" 999 "+marker("FINISHED")+" true";
    }
    
    protected String dir() {
        return "/tmp/"+callback.sessionId+"/"+callback.stepId;
    }
    
    protected String marker(String marker) {
        return marker+"_"+callback.sessionId+"_"+callback.stepId;
    }

    private String getWordAfter(String marker, String output) {
        int index = output.indexOf(marker);
        if (index < 0) {
            throw new IllegalStateException("Unable to find '"+marker+"' in output:\n"+output);
        }
        String next = output.substring(index + marker.length()).trim();
        int firstSpace = 0;
        while (firstSpace < next.length()) {
            if (Character.isWhitespace(next.charAt(firstSpace))) {
                break;
            }
            firstSpace++;
        }
        return next.substring(0, firstSpace);
    }


    /** sets PID in callback; callback should not have a PID */
    public void runOrRejoinAndSetPid(String rawCommand) {
        if (callback.pid!=0) {
            throw new IllegalStateException("Callback specified PID "+callback.pid+" when asked to run '"+rawCommand+"' for "+callback.stepId);
        }
        
        // TODO tidy the below, idea is it runs rawCommand in the BG and this script is idempotent
        // returning the PID
        // ... nohupping script.sh and taking its pid might be better
        
        String script = "cat > script.sh <<EOF \n" +
            "mkdir -p "+dir()+"\n"+
            "cd "+dir()+"\n"+
            "if [ ! -f out ] ; then \n"+
            "  nohup "+rawCommand+" > out 2> err & \n" +
            "  echo $! > pid \n"+
            "fi\n"+
            "echo "+marker("PID")+" `cat pid`\n"+
            "EOF\n"+
            ". ./script.sh\n";
        
        callback.pid = execAndGetPid(script);
    }

    protected int execAndGetPid(String script) {
        String pidS = getWordAfter(marker("PID"), exec(script));
        try {
            return Integer.parseInt(pidS);
        } catch (Exception e) {
            throw new IllegalStateException("Could not find PID in '"+pidS+"': "+e, e);
        }
    }

    /** returns whether this is finished.  if so, stdout and stderr are updated. */
    public boolean checkFinishedAndUpdate() {
        if (callback.pid==0) {
            throw new IllegalStateException("Callback specified PID "+callback.pid+" when asked to check finished for "+callback.stepId);
        }

        boolean isFinished = execIsPidFinished(callback.pid);
        
        if (!isFinished) {
            running = true;
        } else {
            running = false;
            updateStdinStdout();
        }
        return !running;
    }

    protected boolean execIsPidFinished(int pid) {
        return exec("ps -p "+pid + " || echo "+marker("FINISHED")).contains(marker("FINISHED"));
    }

    protected void updateStdinStdout() {
        stdout = exec("cd "+dir()+" && cat out");
        stderr = exec("cd "+dir()+" && cat out");
    }
    
    
    
}

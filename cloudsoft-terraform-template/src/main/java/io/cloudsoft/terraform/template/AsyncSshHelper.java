package io.cloudsoft.terraform.template;

public class AsyncSshHelper {

    String sessionId, stepId;
    
    public static boolean MOCK = true;
    
    boolean running;
    String stdout;
    String stderr;
    
    public AsyncSshHelper(String sessionId, String stepId) {
        this.sessionId = sessionId;
        this.stepId = stepId;
    }
    
    public AsyncSshHelper(CallbackContext callback) {
        this(callback.sessionId, callback.stepId);
    }
    
    protected String exec(String command) {
        // TODO run command over ssh, return the output
        return "TODO";
    }
    
    protected String dir() {
        return "/tmp/"+sessionId+"/"+stepId;
    }
    
    protected String marker(String marker) {
        return marker+"_"+sessionId+"_"+stepId;
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


    /** returns PID */
    public int runOrRejoin(String rawCommand) {
        // TODO tidy the below, idea is it runs rawCommand in the BG and this script is idempotent
        // returning the PID
        
        String script = "cat > script.sh <<EOF \n" +
            "mkdir -p /tmp/"+sessionId+"/"+stepId+"\n"+
            "cd /tmp/"+sessionId+"/"+stepId+"\n" +
            "if [ ! -f out ] ; then \n"+
            "  nohup "+rawCommand+" > out 2> err & \n" +
            "  echo $1 > pid \n"+
            "fi\n"+
            "echo "+marker("PID")+" `cat pid`\n"+
            "EOF\n"+
            ". ./script.sh\n";
        
        // TODO run the above SSH, parse the output for the pid, or throw error
        int pid;
        if (MOCK) {
            pid = 999;
        } else {
            String pidS = getWordAfter(marker("PID"), exec(script));
            try {
                pid = Integer.parseInt(pidS);
            } catch (Exception e) {
                throw new IllegalStateException("Could not find PID in '"+pidS+"': "+e, e);
            }
        }
        
        return pid;
    }

    /** returns whether this is finished.  if so, stdout and stderr are updated. */
    public boolean update(int pid) {
        boolean isFinished = 
            MOCK ? Math.random()<0.3 : exec("ps -p "+pid + " || echo "+marker("FINISHED")).contains(marker("FINISHED"));
        
        if (!isFinished) {
            running = true;
        } else {
            running = false;
            updateStdinStdout();
        }
        return !running;
    }

    protected void updateStdinStdout() {
        stdout = exec("cd "+dir()+" && cat out");
        stderr = exec("cd "+dir()+" && cat out");
    }
    
    
    
}

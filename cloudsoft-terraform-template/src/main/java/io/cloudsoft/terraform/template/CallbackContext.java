package io.cloudsoft.terraform.template;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CallbackContext {

    String sessionId;
    String stepId;
    int pid;
    int lastDelaySeconds;
    boolean forceSynchronous = false;
    boolean disregardCallbackDelay = false; // Call the worker again ASAP (it is not a good idea).
    
}

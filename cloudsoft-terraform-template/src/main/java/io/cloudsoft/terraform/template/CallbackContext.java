package io.cloudsoft.terraform.template;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CallbackContext {

    public String stepId;
    public int lastDelaySeconds;
    public boolean forceSynchronous = false;
    public boolean disregardCallbackDelay = false; // Call the worker again ASAP (it is not a good idea).
    
}

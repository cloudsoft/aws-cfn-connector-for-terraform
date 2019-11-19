package io.cloudsoft.terraform.infrastructure;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CallbackContext {

    public String stepId;
    public int lastDelaySeconds;

    // for testing:
    public boolean forceSynchronous = false;
    public boolean disregardCallbackDelay = false; // Call the worker again ASAP (use with care)

}

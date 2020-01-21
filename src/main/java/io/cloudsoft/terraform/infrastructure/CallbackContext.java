package io.cloudsoft.terraform.infrastructure;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CallbackContext {

    public String stepId;
    public int lastDelaySeconds;
    
    // needed for creation only
    public String createdModelIdentifier;
    public String processManager;

}

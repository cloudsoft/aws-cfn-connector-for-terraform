package io.cloudsoft.terraform.infrastructure;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CallbackContext {

    public String stepId;
    public int lastDelaySeconds;
    public String createdModelIdentifier;
    public String processManager;

}

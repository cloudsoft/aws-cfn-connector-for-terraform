package io.cloudsoft.terraform.infrastructure;

import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@Data
@NoArgsConstructor
public class CallbackContext {

    /** This ID is new for each sequence of steps requested by a client, eg a CREATE, an UPDATE, another UPDATE
     * will all have different IDs (but it will be the same in each step of that ID).
     * It looks like {@link ResourceHandlerRequest#getClientRequestToken()} would serve the same purpose but not 100% sure.
     * <p>
     * This is in contrast to {@link ResourceModel#getIdentifier()} which is the same for all steps against the same element in a stack.
     * <p>
     * We do have access to the element name in the TF stack {@link ResourceHandlerRequest#getLogicalResourceIdentifier()}, 
     * but unfortunately it seems we aren't able to know to the stack UID or stack name (hence the choice to include 
     * the date in the model identifier for easier identification).
     */
    public String commandRequestId;
    public String stepId;
    public int lastDelaySeconds;
    
    // cache this for the duration of a command
    public String processManager;
    
    // needed for creation only
    public String createdModelIdentifier;

}

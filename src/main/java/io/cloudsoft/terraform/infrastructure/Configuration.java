package io.cloudsoft.terraform.infrastructure;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Map;

class Configuration extends BaseConfiguration {

    public Configuration() {
        super("cloudsoft-terraform-infrastructure.json");
    }

    public JSONObject resourceSchemaJSONObject() {
        return new JSONObject(new JSONTokener(this.getClass().getClassLoader().getResourceAsStream(schemaFilename)));
    }

    /**
     * Providers should implement this method if their resource has a 'Tags' property to define resource-level tags * @return
     */
    public Map<String, String> resourceDefinedTags(final ResourceModel resourceModel) {
        return null;
    }
}

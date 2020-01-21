package io.cloudsoft.terraform.infrastructure;

import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

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
    
    public static String getDateTimeString() {
        String pattern = "yyyyMMdd-HHmmss";
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = df.format(new Date());
        return date;
    }
    
    public static String getIdentifier(boolean includeDateTime, int randomLength) {
        return 
            (includeDateTime ? getDateTimeString()+"-" : "") 
            + RandomStringUtils.randomAlphanumeric(randomLength);
    }

}

package io.cloudsoft.terraform.template;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TerraformBaseHandler<T> extends BaseHandler<T> {

    private static final String PREFIX = "cfn/terraform";
    private AWSSimpleSystemsManagement awsSimpleSystemsManagement;

    private AmazonS3 amazonS3;
    private Pattern s3Pattern;

    public TerraformBaseHandler(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
        s3Pattern = Pattern.compile("^s3://([^/]*)/(.*)$");
        awsSimpleSystemsManagement = AWSSimpleSystemsManagementClientBuilder.defaultClient();
    }

    public TerraformBaseHandler() {
        this(AmazonS3ClientBuilder.defaultClient());
    }

    protected String getHost() {
        return getParameterValue("ssh-host");
    }

    protected String getPort() {
        return getParameterValue("ssh-port");
    }

    protected String getUsername() {
        return getParameterValue("ssh-username");
    }

    protected String getSSHKey() {
        return getParameterValue("ssh-key");
    }

    protected String getFingerprint() {
        return getParameterValue("ssh-fingerprint");
    }

    private String getParameterValue(String id) {
        GetParameterResult getParameterResult = awsSimpleSystemsManagement.getParameter(new GetParameterRequest()
                .withName(String.format("%s/%s", PREFIX, id))
                .withWithDecryption(true));

        return getParameterResult.getParameter().getValue();
    }

    protected String getConfiguration(ResourceModel model) {
        if (model.getConfigurationContent() != null) {
            return model.getConfigurationContent();
        }

        if (model.getConfigurationUrl() != null) {
            try {
                InputStream inputStream = null;
                inputStream = new URL(model.getConfigurationUrl()).openStream();
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format("Failed to download file at %s", model.getConfigurationUrl()), e);
            }
        }

        if (model.getConfigurationS3Path() != null) {
            Matcher matcher = s3Pattern.matcher(model.getConfigurationS3Path());
            if (!matcher.find()) {
                throw new IllegalArgumentException(String.format("Invalid S3 path %s", model.getConfigurationS3Path()));
            }

            String bucket = matcher.group(1);
            String key = matcher.group(2);

            try {
                S3Object templateObject = amazonS3.getObject(new GetObjectRequest(bucket, key));
                return IOUtils.toString(templateObject.getObjectContent(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("Failed to get S3 file at %s", model.getConfigurationS3Path()), e);
            }
        }

        throw new IllegalStateException("Missing one of the template properties");
    }
}

package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

public class TerraformParameters {

    private static final String PREFIX = "/cfn/terraform";
    private static final int DEFAULT_SSH_PORT = 22;
    private static final String DEFAULT_PROCESS_MANAGER = "nohup";
    // allow this so that parameters can be set, as they don't allow blanks or null
    private static final Set<String> DEFAULT_KEYWORDS = new LinkedHashSet<String>(Arrays.asList("default", "disabled", "off"));
    private Logger logger;
    private final AmazonWebServicesClientProxy proxy;
    private final SsmClient ssmClient;
    private final S3Client s3Client;

    public TerraformParameters(Logger logger, AmazonWebServicesClientProxy proxy, SsmClient ssmClient, S3Client s3Client) {
        this.logger = logger;
        this.proxy = proxy;
        this.ssmClient = ssmClient;
        this.s3Client = s3Client;
    }

    public TerraformParameters(Logger logger, AmazonWebServicesClientProxy proxy) {
        this(logger, proxy, SsmClient.create(), S3Client.create());
    }
    
    protected boolean isDefault(Object x) {
        return x==null || DEFAULT_KEYWORDS.contains(x.toString().toLowerCase());
    }

    public String getHost() {
        return getParameterValue("ssh-host", true);
    }

    public int getPort() {
        final String port = getParameterValue("ssh-port", false);
        if (isDefault(port)) {
            return DEFAULT_SSH_PORT;
        }
        try {
            return Integer.parseInt(port.trim());

        } catch (Exception e) {
            throw ConnectorHandlerFailures.unhandled("Parameter 'ssh-port' is invalid: '"+port+"'");

        }
    }

    public String getProcessManager() {
        String pm = getParameterValue("process-manager", false);
        if (isDefault(pm)) {
            pm = DEFAULT_PROCESS_MANAGER;
        }
        if (pm.equals("systemd") || pm.equals("nohup")) {
            return pm;
        }
        throw ConnectorHandlerFailures.unhandled("Parameter 'process-manager' is invalid: '" + pm + "'");
    }

    public String getUsername() {
        return getParameterValue("ssh-username", true);
    }

    public String getSSHKey() {
        return getParameterValue("ssh-key", true);
    }

    public String getFingerprint() {
        String fp = getParameterValue("ssh-fingerprint", false);
        if (isDefault(fp)) {
            return null;
        }
        return fp;
    }

    public String getLogsS3BucketPrefix() {
        String bp = getParameterValue("logs-s3-bucket-prefix", false);
        if (isDefault(bp)) {
            return null;
        }
        return bp;
    }

    private String getParameterValue(String id, boolean required) {
        GetParameterRequest getParameterRequest = GetParameterRequest.builder()
                .name(PREFIX + "/" + id)
                .withDecryption(true)
                .build();

        try {
            GetParameterResponse getParameterResponse = proxy.injectCredentialsAndInvokeV2(getParameterRequest,
                ssmClient::getParameter);
            return getParameterResponse.parameter().value();

        } catch (ParameterNotFoundException e) {
            if (required) {
                throw ConnectorHandlerFailures.unhandled("Parameter '"+id+"' must be set in parameter store.", e);
            } else {
                // annoyingly we get failure messages in the log if the parameter doesn't exist; explain that so people don't panic
                if (logger!=null) {
                    logger.log("Parameter '"+id+"' not in parameter store; using default. If there is an SSM failure message above, it is likely due to this and is benign. Set the default explicitly to suppress these messages.");
                }
                return null;
            }
        } catch (RuntimeException e) {
            throw ConnectorHandlerFailures.unhandled("Parameter '"+id+"' could not be retrieved; "
                + "check roles/permissions are set for this type connector: "+e, e);
        }
    }

    public byte[] getConfiguration(ResourceModel model) {
        if (model.getConfigurationContent() != null) {
            return model.getConfigurationContent().getBytes(StandardCharsets.UTF_8);
        }

        if (model.getConfigurationUrl() != null) {
            try {
                return IOUtils.toByteArray(new URL(model.getConfigurationUrl()));
            } catch (IOException e) {
                throw ConnectorHandlerFailures.unhandled("Failed to download file at " + model.getConfigurationUrl(), e);
            }
        }

        if (model.getConfigurationS3Path() != null) {
            final Pattern s3Pattern = Pattern.compile("^s3://([^/]*)/(.*)$");
            final Matcher matcher = s3Pattern.matcher(model.getConfigurationS3Path());
            if (!matcher.find()) {
                throw ConnectorHandlerFailures.unhandled("Invalid S3 path " + model.getConfigurationS3Path());
            }

            final String bucket = matcher.group(1);
            final String key = matcher.group(2);

            try {
                byte[] result = new BucketUtils(proxy, s3Client).download(bucket, key);
                if (result.length==0) {
                    throw ConnectorHandlerFailures.unhandled(String.format("S3 file at %s is empty", model.getConfigurationS3Path()));
                }
                return result;
            } catch (Exception e) {
                throw ConnectorHandlerFailures.unhandled(String.format("Failed to get S3 Terraform configuration file at %s: check it exists and roles/permissions set for this type connector", model.getConfigurationS3Path()), e);
            }
        }

        throw ConnectorHandlerFailures.unhandled("No Configuration properties are set.");
    }

}

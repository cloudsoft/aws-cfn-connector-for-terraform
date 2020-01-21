package io.cloudsoft.terraform.infrastructure;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TerraformParameters {

    private static final String PREFIX = "/cfn/terraform";
    private final AmazonWebServicesClientProxy proxy;
    private final SsmClient ssmClient;
    private final S3Client s3Client;

    public TerraformParameters(AmazonWebServicesClientProxy proxy, SsmClient ssmClient, S3Client s3Client) {
        this.proxy = proxy;
        this.ssmClient = ssmClient;
        this.s3Client = s3Client;
    }

    public TerraformParameters(AmazonWebServicesClientProxy proxy) {
        this(proxy, SsmClient.create(), S3Client.create());
    }

    public String getHost() {
        return getParameterValue("ssh-host", true);
    }

    public int getPort() {
        final String port = getParameterValue("ssh-port", false);
        if (port==null) {
            return 22;
        }
        try {
            return Integer.parseInt(port.trim());

        } catch (Exception e) {
            throw ConnectorHandlerFailures.unhandled("Parameter 'ssh-port' is invalid: '"+port+"'");

        }
    }

    public String getProcessManager() {
        return "systemd"; // FIXME: retrieve with getParameterValue() and validate
    }

    public String getUsername() {
        return getParameterValue("ssh-username", true);
    }

    public String getSSHKey() {
        return getParameterValue("ssh-key", true);
    }

    public String getFingerprint() {
        return getParameterValue("ssh-fingerprint", false);
    }

    public String getLogsS3BucketName() {
        return getParameterValue("logs-s3-bucket-name", false);
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
                final File tmpFile = File.createTempFile("configuration-", ".tf");
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();
                proxy.injectCredentialsAndInvokeV2(getObjectRequest, request -> s3Client.getObject(request, tmpFile.toPath()));
                final byte[] result = FileUtils.readFileToByteArray(tmpFile);
                if (result.length==0) {
                    throw ConnectorHandlerFailures.unhandled(String.format("S3 file at %s is empty", model.getConfigurationS3Path()));
                }
                return result;
            } catch (Exception e) {
                throw ConnectorHandlerFailures.unhandled(String.format("Failed to get S3 file at %s: check it exists and roles/permissions set for this type connector", model.getConfigurationS3Path()), e);
            }
        }

        throw ConnectorHandlerFailures.unhandled("No Configuration properties are set.");
    }

}

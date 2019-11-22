package io.cloudsoft.terraform.infrastructure;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import io.cloudsoft.terraform.infrastructure.ResourceModel;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

public class TerraformParameters {

    private static final String PREFIX = "/cfn/terraform";
    private final AmazonWebServicesClientProxy proxy;
    private final SsmClient ssmClient;
    private final S3Client s3Client;
    private final Pattern s3Pattern;

    public TerraformParameters(AmazonWebServicesClientProxy proxy, SsmClient ssmClient, S3Client s3Client) {
        this.proxy = proxy;
        this.ssmClient = ssmClient;
        this.s3Client = s3Client;
        s3Pattern = Pattern.compile("^s3://([^/]*)/(.*)$");
    }

    public TerraformParameters(AmazonWebServicesClientProxy proxy) {
        this(proxy, SsmClient.create(), S3Client.create());
    }

    public String getHost() {
        return getParameterValue("ssh-host");
    }

    public int getPort() {
        int ret;
        try {
            ret = Integer.parseInt(getParameterValue("ssh-port").trim());
        } catch (Exception e) {
            // if not set or not an integer
            // TODO if the exception is anything other than whatever is returned for a missing parameter, log it
            ret = 22;
        }
        return ret;
    }

    public String getUsername() {
        return getParameterValue("ssh-username");
    }

    public String getSSHKey() {
        return getParameterValue("ssh-key");
    }

    public String getFingerprint() {
        return getParameterValue("ssh-fingerprint");
    }

    public String getParameterValue(String id) {
        GetParameterRequest getParameterRequest = GetParameterRequest.builder()
                .name(String.format("%s/%s", PREFIX, id))
                .withDecryption(true)
                .build();

        GetParameterResponse getParameterResponse = proxy.injectCredentialsAndInvokeV2(getParameterRequest,
                ssmClient::getParameter);

        return getParameterResponse.parameter().value();
    }

    public byte[] getConfiguration(ResourceModel model) {
        if (model.getConfigurationContent() != null) {
            return model.getConfigurationContent().getBytes(StandardCharsets.UTF_8);
        }

        if (model.getConfigurationUrl() != null) {
            try {
                return IOUtils.toByteArray(new URL(model.getConfigurationUrl()));
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
                File tmpFile = File.createTempFile("configuration-", ".tf");
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build();
                proxy.injectCredentialsAndInvokeV2(getObjectRequest, request -> s3Client.getObject(request, tmpFile.toPath()));
                byte[] result = FileUtils.readFileToByteArray(tmpFile);
                if (result.length==0) {
                    throw new IllegalArgumentException(String.format("S3 file at %s is empty", model.getConfigurationS3Path()));
                }
                return result;
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("Failed to get S3 file at %s", model.getConfigurationS3Path()), e);
            }
        }

        throw new IllegalStateException("Missing one of the configuration properties");
    }

}

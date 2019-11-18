package io.cloudsoft.terraform.template;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import io.cloudsoft.terraform.template.worker.AbstractHandlerWorker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

public abstract class TerraformBaseHandler<T> extends BaseHandler<T> {
    private static final String PREFIX = "/cfn/terraform";
    private SsmClient ssmClient;
    private S3Client s3Client;
    private Pattern s3Pattern;

    public TerraformBaseHandler(SsmClient ssmClient, S3Client s3Client) {
        this.ssmClient = ssmClient;
        this.s3Client = s3Client;
        s3Pattern = Pattern.compile("^s3://([^/]*)/(.*)$");
    }

    public TerraformBaseHandler() {
        this(SsmClient.create(), S3Client.create());
    }

    protected String getHost(AmazonWebServicesClientProxy proxy) {
        return getParameterValue(proxy, "ssh-host");
    }

    protected int getPort(AmazonWebServicesClientProxy proxy) {
        int ret;
        try {
            ret = Integer.parseInt(getParameterValue(proxy, "ssh-port").trim());
        } catch (Exception e) {
            // if not set or not an integer
            // TODO if the exception is anything other than whatever is returned for a missing parameter, log it
            ret = 22;
        }
        return ret;
    }

    protected String getUsername(AmazonWebServicesClientProxy proxy) {
        return getParameterValue(proxy, "ssh-username");
    }

    protected String getSSHKey(AmazonWebServicesClientProxy proxy) {
        return getParameterValue(proxy, "ssh-key");
    }

    protected String getFingerprint(AmazonWebServicesClientProxy proxy) {
        return getParameterValue(proxy, "ssh-fingerprint");
    }

    private String getParameterValue(AmazonWebServicesClientProxy proxy, String id) {
        GetParameterRequest getParameterRequest = GetParameterRequest.builder()
                .name(String.format("%s/%s", PREFIX, id))
                .withDecryption(true)
                .build();

        GetParameterResponse getParameterResponse = proxy.injectCredentialsAndInvokeV2(getParameterRequest,
                ssmClient::getParameter);

        return getParameterResponse.parameter().value();
    }

    public byte[] getConfiguration(AmazonWebServicesClientProxy proxy, ResourceModel model) {
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
                return FileUtils.readFileToByteArray(tmpFile);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("Failed to get S3 file at %s", model.getConfigurationS3Path()), e);
            }
        }

        throw new IllegalStateException("Missing one of the template properties");
    }

    protected ProgressEvent<ResourceModel, CallbackContext> run(CallbackContext callback, Function<CallbackContext,AbstractHandlerWorker> workerFactory) {
        // allows us to force synchronous behaviour -- especially useful when running in SAM
        boolean forceSynchronous = callback == null ? false : callback.forceSynchronous;
        boolean disregardCallbackDelay = callback == null ? false : callback.disregardCallbackDelay;

        while (true) {
            AbstractHandlerWorker worker = workerFactory.apply(callback);
            ProgressEvent<ResourceModel, CallbackContext> result = worker.call();
            if (!forceSynchronous || !OperationStatus.IN_PROGRESS.equals(result.getStatus())) {
                return result;
            }
            worker.log("Synchronous mode: "+result.getCallbackContext());
            try {
                if (disregardCallbackDelay) {
                    worker.log("Will run callback immediately");
                } else {
                    worker.log("Will run callback after "+result.getCallbackDelaySeconds()+" seconds");
                    Thread.sleep(1000*result.getCallbackDelaySeconds());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Aborted due to interrupt", e);
            }
            callback = result.getCallbackContext();
        }
    }
}

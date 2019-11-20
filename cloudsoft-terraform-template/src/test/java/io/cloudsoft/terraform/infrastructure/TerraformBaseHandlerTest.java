package io.cloudsoft.terraform.infrastructure;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import software.amazon.awssdk.awscore.AwsResponseMetadata;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.SsmResponse;
import software.amazon.awssdk.services.ssm.model.SsmResponseMetadata;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TerraformBaseHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private SsmClient ssmClient;

    @Mock
    private S3Client s3Client;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        ssmClient = mock(SsmClient.class);
        s3Client = mock(S3Client.class);
    }

    @Test
    public void getHostReturnsParameterFromParameterStore() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-host")
                .withDecryption(true)
                .build();
        final String expected = "acme.com";

        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).thenReturn(GetParameterResponse.builder()
                .parameter(Parameter.builder()
                        .value(expected)
                        .build())
                .build());

        String host = handler.getHost(proxy);
        assertEquals(expected, host);
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
    }

    @Test
    public void getFingerprintReturnsParameterFromParameterStore() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-fingerprint")
                .withDecryption(true)
                .build();
        final String expected = "12:f8:7e:78:61:b4:bf:e2:de:24:15:96:4e:d4:72:53";

        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).thenReturn(GetParameterResponse.builder()
                .parameter(Parameter.builder()
                        .value(expected)
                        .build())
                .build());

        String fingerprint = handler.getFingerprint(proxy);
        assertEquals(expected, fingerprint);
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
    }

    @Test
    public void getSSHKeyReturnsParameterFromParameterStore() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-key")
                .withDecryption(true)
                .build();
        final String expected = "=== RSA KEY === .............";

        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).thenReturn(GetParameterResponse.builder().parameter(Parameter.builder().value(expected).build()).build());

        String sshKey = handler.getSSHKey(proxy);
        assertEquals(expected, sshKey);
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
    }

    @Test
    public void getUsernameReturnsParameterFromParameterStore() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-username")
                .withDecryption(true)
                .build();
        final String expected = "root";

        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).thenReturn(GetParameterResponse.builder().parameter(Parameter.builder().value(expected).build()).build());

        String username = handler.getUsername(proxy);
        assertEquals(expected, username);
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
    }

    @Test
    public void getPortReturnsParameterFromParameterStore() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-port")
                .withDecryption(true)
                .build();
        final String expected = "22";

        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).thenReturn(GetParameterResponse.builder().parameter(Parameter.builder().value(expected).build()).build());

        int port = handler.getPort(proxy);
        assertEquals(Integer.parseInt(expected), port);
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
    }

    @Test
    public void getHostThrowsIfGetParameterThrows() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-host")
                .withDecryption(true)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).then(InvocationOnMock::callRealMethod);
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> {
            handler.getHost(proxy);
        });
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
        verify(ssmClient, times(1)).getParameter(any(GetParameterRequest.class));
    }

    @Test
    public void getFingerprintThrowsIfGetParameterThrows() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-fingerprint")
                .withDecryption(true)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).then(InvocationOnMock::callRealMethod);
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> {
            handler.getFingerprint(proxy);
        });
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
        verify(ssmClient, times(1)).getParameter(any(GetParameterRequest.class));
    }

    @Test
    public void getSSHKeyThrowsIfGetParameterThrows() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-key")
                .withDecryption(true)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).then(InvocationOnMock::callRealMethod);
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> {
            handler.getSSHKey(proxy);
        });
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
        verify(ssmClient, times(1)).getParameter(any(GetParameterRequest.class));
    }

    @Test
    public void getUsernameThrowsIfGetParameterThrows() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-username")
                .withDecryption(true)
                .build();
        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).then(InvocationOnMock::callRealMethod);
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(new RuntimeException());

        assertThrows(RuntimeException.class, () -> {
            handler.getUsername(proxy);
        });
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
        verify(ssmClient, times(1)).getParameter(any(GetParameterRequest.class));
    }

    @Test
    public void getPortReturns22IfGetParameterThrows() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-port")
                .withDecryption(true)
                .build();
        final int expected = 22;
        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).then(InvocationOnMock::callRealMethod);
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(new RuntimeException());

        int port = handler.getPort(proxy);
        assertEquals(expected, port);
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
        verify(ssmClient, times(1)).getParameter(any(GetParameterRequest.class));
    }

    @Test
    public void getConfigurationReturnConfigurationContentProperty() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final String configurationContent = "Hello world";
        final ResourceModel model = ResourceModel.builder().configurationContent(configurationContent).build();

        byte[] result = handler.getConfiguration(proxy, model);
        assertArrayEquals(configurationContent.getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    public void getConfigurationReturnsDownloadedConfigurationFromUrlProperty() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final String configurationUrl = "http://www.mocky.io/v2/5dc19cab33000051e91a5437";
        final ResourceModel model = ResourceModel.builder().configurationUrl(configurationUrl).build();

        String expected = "Hello world";
        byte[] result = handler.getConfiguration(proxy, model);

        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    public void getConfigurationThrowsIfUrlDoesNotExistProperty() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final String configurationUrl = "http://acme.com/does/not/exist";
        final ResourceModel model = ResourceModel.builder().configurationUrl(configurationUrl).build();

        assertThrows(IllegalArgumentException.class, () -> {
            handler.getConfiguration(proxy, model);
        });
    }

    @Test
    public void getConfigurationReturnsDownloadedConfigurationFromS3PathProperty() {
        final String expectedBucket = "my-bucket";
        final String expectedKey = "hello-world.txt";
        final String expectedContent = "Hello world";
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(ssmClient, s3Client);
        final String configurationS3Path = String.format("s3://%s/%s", expectedBucket, expectedKey);
        final ResourceModel model = ResourceModel.builder().configurationS3Path(configurationS3Path).build();

        when(proxy.injectCredentialsAndInvokeV2(any(GetObjectRequest.class), any())).then(InvocationOnMock::callRealMethod);
        when(s3Client.getObject(any(GetObjectRequest.class), any(Path.class))).then(invocationOnMock -> {
            Path path = invocationOnMock.getArgument(1);
            FileUtils.write(path.toFile(), expectedContent, StandardCharsets.UTF_8);
            return null;
        });

        byte[] result = handler.getConfiguration(proxy, model);
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(any(GetObjectRequest.class), any());
        ArgumentCaptor<GetObjectRequest> argument = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client, times(1)).getObject(argument.capture(), any(Path.class));

        assertArrayEquals(expectedContent.getBytes(StandardCharsets.UTF_8), result);
        assertEquals(expectedBucket, argument.getValue().bucket());
        assertEquals(expectedKey, argument.getValue().key());
    }

    @Test
    public void getConfigurationThrowsIfS3PathIsInvalid() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest();
        final String configurationS3Path = "http://acme.com/does/not/exist";
        final ResourceModel model = ResourceModel.builder().configurationS3Path(configurationS3Path).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).then(InvocationOnMock::callRealMethod);

        assertThrows(IllegalArgumentException.class, () -> {
            handler.getConfiguration(proxy, model);
        });
    }

    @Test
    public void getConfigurationThrowsIfS3PathDoesNotExistProperty() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest();
        final String configurationS3Path = "s3://bucket/does/not/exist";
        final ResourceModel model = ResourceModel.builder().configurationS3Path(configurationS3Path).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).then(InvocationOnMock::callRealMethod);

        assertThrows(IllegalArgumentException.class, () -> {
            handler.getConfiguration(proxy, model);
        });
    }

    @Test
    public void getConfigurationThrowsIfNoProperties() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest();
        final ResourceModel model = ResourceModel.builder().build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).then(InvocationOnMock::callRealMethod);

        assertThrows(IllegalStateException.class, () -> {
            handler.getConfiguration(proxy, model);
        });
    }

    // TerraformBaseHandler is an abstract class. In order to tests methods within it, we need to create a dummy implementation
    class TerraformBaseHandlerUnderTest extends TerraformBaseHandler<CallbackContext> {
        public TerraformBaseHandlerUnderTest() {
            super();
        }

        public TerraformBaseHandlerUnderTest(SsmClient awsSimpleSystemsManagement, S3Client amazonS3) {
            super(awsSimpleSystemsManagement, amazonS3);
        }

        @Override
        public ProgressEvent<ResourceModel, CallbackContext> handleRequest(AmazonWebServicesClientProxy proxy,
                                                                           ResourceHandlerRequest<ResourceModel> request,
                                                                           CallbackContext callbackContext, Logger logger) {
            return null;
        }
    }

    // TODO: Add tests for run()
}

package io.cloudsoft.terraform.infrastructure;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;

import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

public class TerraformParametersTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private SsmClient ssmClient;

    @Mock
    private S3Client s3Client;

    private TerraformParameters parameters;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        ssmClient = mock(SsmClient.class);
        s3Client = mock(S3Client.class);
        parameters = new TerraformParameters(null, proxy, ssmClient, s3Client);
    }

    @Test
    public void getHostReturnsParameterFromParameterStore() {
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

        String host = parameters.getHost();
        assertEquals(expected, host);
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
    }

    @Test
    public void getFingerprintReturnsParameterFromParameterStore() {
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-fingerprint")
                .withDecryption(true)
                .build();
        final String expected = "12:f8:7e:78:61:b4:bf:e2:de:24:15:96:4e:d4:72:53";

        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).thenReturn(
            GetParameterResponse.builder()
                .parameter(Parameter.builder()
                        .value(expected)
                        .build())
                .build());

        String fingerprint = parameters.getFingerprint();
        assertEquals(expected, fingerprint);
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
    }

    @Test
    public void getSSHKeyReturnsParameterFromParameterStore() {
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-key")
                .withDecryption(true)
                .build();
        final String expected = "=== RSA KEY === .............";

        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).thenReturn(
            GetParameterResponse.builder().parameter(Parameter.builder().value(expected).build()).build());

        String sshKey = parameters.getSSHKey();
        assertEquals(expected, sshKey);
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
    }

    @Test
    public void getUsernameReturnsParameterFromParameterStore() {
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-username")
                .withDecryption(true)
                .build();
        final String expected = "root";

        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).thenReturn(
            GetParameterResponse.builder().parameter(Parameter.builder().value(expected).build()).build());

        assertEquals(expected, parameters.getUsername());
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
    }

    @Test
    public void getPortReturnsParameterFromParameterStore() {
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-port")
                .withDecryption(true)
                .build();
        final String expected = "1234";

        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).thenReturn(
            GetParameterResponse.builder().parameter(Parameter.builder().value(expected).build()).build());

        assertEquals(Integer.parseInt(expected), parameters.getPort());
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
    }

    @Test
    public void getPortReturns22IfNotFound() {
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-port")
                .withDecryption(true)
                .build();
        whenProxyGetParameterCallSsmGetParameter();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(ParameterNotFoundException.builder().build());

        assertEquals(22, parameters.getPort());
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
    }

    @Test
    public void getPortThrowsIfGetParameterThrowsOtherError() {
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-port")
                .withDecryption(true)
                .build();
        whenProxyGetParameterCallSsmGetParameter();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(new RuntimeException());

        assertThrows(ConnectorHandlerFailures.Unhandled.class, () -> {
            parameters.getPort();
        });
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
        verify(ssmClient, times(1)).getParameter(any(GetParameterRequest.class));
    }

    @Test
    public void getHostThrowsIfGetParameterThrows() {
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-host")
                .withDecryption(true)
                .build();
        whenProxyGetParameterCallSsmGetParameter();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(ParameterNotFoundException.builder().build());

        assertThrows(ConnectorHandlerFailures.Unhandled.class, () -> {
            parameters.getHost();
        });
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
        verify(ssmClient, times(1)).getParameter(any(GetParameterRequest.class));
    }

    @Test
    public void getFingerprintReturnsNullIfGetParameterThrowsNotFound() {
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-fingerprint")
                .withDecryption(true)
                .build();
        whenProxyGetParameterCallSsmGetParameter();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(ParameterNotFoundException.builder().build());

        assertEquals(null, parameters.getFingerprint());
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
        verify(ssmClient, times(1)).getParameter(any(GetParameterRequest.class));
    }

    @Test
    public void getFingerprintThrowsIfGetParameterThrowsOtherError() {
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-fingerprint")
                .withDecryption(true)
                .build();
        whenProxyGetParameterCallSsmGetParameter();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(new RuntimeException());

        assertThrows(ConnectorHandlerFailures.Unhandled.class, () -> {
            parameters.getFingerprint();
        });
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
        verify(ssmClient, times(1)).getParameter(any(GetParameterRequest.class));
    }

    @Test
    public void getSSHKeyThrowsIfGetParameterThrows() {
        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-key")
                .withDecryption(true)
                .build();
        whenProxyGetParameterCallSsmGetParameter();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(ParameterNotFoundException.builder().build());

        assertThrows(ConnectorHandlerFailures.Unhandled.class, () -> {
            parameters.getSSHKey();
        });
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
        verify(ssmClient, times(1)).getParameter(any(GetParameterRequest.class));
    }

    @Test
    public void getUsernameThrowsIfGetParameterThrows() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        ssmClient = mock(SsmClient.class);
        s3Client = mock(S3Client.class);
        parameters = new TerraformParameters(null, proxy, ssmClient, s3Client);

        final GetParameterRequest expectedGetParameterRequest = GetParameterRequest.builder()
                .name("/cfn/terraform/ssh-username")
                .withDecryption(true)
                .build();
        whenProxyGetParameterCallSsmGetParameter();
        when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(ParameterNotFoundException.builder().build());

        assertThrows(ConnectorHandlerFailures.Unhandled.class, () -> {
            parameters.getUsername();
        });
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(eq(expectedGetParameterRequest), any());
        verify(ssmClient, times(1)).getParameter(any(GetParameterRequest.class));
    }

    protected void whenProxyGetParameterCallSsmGetParameter() {
        // if we just do InvocationOnMock::callRealMethod we get NPE due to loggerProxy
        when(proxy.injectCredentialsAndInvokeV2(any(GetParameterRequest.class), any())).then(
            ctx -> ssmClient.getParameter((GetParameterRequest)ctx.getArgument(0)));
    }

    @Test
    public void getConfigurationReturnConfigurationContentProperty() {
        final String configurationContent = "Hello world";
        final ResourceModel model = ResourceModel.builder().configurationContent(configurationContent).build();

        byte[] result = parameters.getConfiguration(model);
        assertArrayEquals(configurationContent.getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    public void getConfigurationReturnsDownloadedConfigurationFromUrlProperty() {
        final String configurationUrl = "http://www.mocky.io/v2/5dc19cab33000051e91a5437";
        final ResourceModel model = ResourceModel.builder().configurationUrl(configurationUrl).build();

        String expected = "Hello world";
        byte[] result = parameters.getConfiguration(model);

        assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    public void getConfigurationThrowsIfUrlDoesNotExistProperty() {
        final String configurationUrl = "http://acme.com/does/not/exist";
        final ResourceModel model = ResourceModel.builder().configurationUrl(configurationUrl).build();

        assertThrows(ConnectorHandlerFailures.Unhandled.class, () -> {
            parameters.getConfiguration(model);
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void getConfigurationReturnsDownloadedConfigurationFromS3PathProperty() {
        final String expectedBucket = "my-bucket";
        final String expectedKey = "hello-world.txt";
        final String expectedContent = "Hello world";
        final String configurationS3Path = String.format("s3://%s/%s", expectedBucket, expectedKey);
        final ResourceModel model = ResourceModel.builder().configurationS3Path(configurationS3Path).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).then(InvocationOnMock::callRealMethod);
        when(s3Client.getObject(any(GetObjectRequest.class), any(ResponseTransformer.class))).then(invocationOnMock -> {
            ResponseTransformer<?,?> transformer = invocationOnMock.getArgument(1);
            transformer.transform(null, AbortableInputStream.create(new ByteArrayInputStream(expectedContent.getBytes())));
            return null;
        });

        byte[] result = parameters.getConfiguration(model);
        verify(proxy, times(1)).injectCredentialsAndInvokeV2(any(GetObjectRequest.class), any());
        ArgumentCaptor<GetObjectRequest> argument = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client, times(1)).getObject(argument.capture(), any(ResponseTransformer.class));

        assertArrayEquals(expectedContent.getBytes(StandardCharsets.UTF_8), result);
        assertEquals(expectedBucket, argument.getValue().bucket());
        assertEquals(expectedKey, argument.getValue().key());
    }

    @Test
    public void getConfigurationThrowsIfS3PathIsInvalid() {
        final String configurationS3Path = "http://acme.com/does/not/exist";
        final ResourceModel model = ResourceModel.builder().configurationS3Path(configurationS3Path).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).then(InvocationOnMock::callRealMethod);

        assertThrows(ConnectorHandlerFailures.Unhandled.class, () -> {
            parameters.getConfiguration(model);
        });
    }

    @Test
    public void getConfigurationThrowsIfS3PathDoesNotExistProperty() {
        final String configurationS3Path = "s3://bucket/does/not/exist";
        final ResourceModel model = ResourceModel.builder().configurationS3Path(configurationS3Path).build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).then(InvocationOnMock::callRealMethod);
        when(s3Client.getObject((GetObjectRequest)any(), (Path)any())).thenThrow(IllegalArgumentException.class);

        assertThrows(ConnectorHandlerFailures.Unhandled.class, () -> {
            parameters.getConfiguration(model);
        });
    }

    @Test
    public void getConfigurationThrowsIfNoProperties() {
        final ResourceModel model = ResourceModel.builder().build();

        when(proxy.injectCredentialsAndInvokeV2(any(), any())).then(InvocationOnMock::callRealMethod);

        assertThrows(ConnectorHandlerFailures.Unhandled.class, () -> {
            parameters.getConfiguration(model);
        });
    }
}

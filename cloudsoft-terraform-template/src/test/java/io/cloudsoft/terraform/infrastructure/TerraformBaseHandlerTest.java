package io.cloudsoft.terraform.infrastructure;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

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

    // TODO: Add tests about getParameters
    // TODO: Add tests for run()
}

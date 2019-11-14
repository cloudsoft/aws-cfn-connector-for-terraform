package io.cloudsoft.terraform.template;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;

import static junit.framework.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TerraformBaseHandlerTest {

    @Mock
    private AWSSimpleSystemsManagement awsSimpleSystemsManagement;

    @Mock
    private AmazonS3 amazonS3;

    @BeforeEach
    public void setup() {
        awsSimpleSystemsManagement = mock(AWSSimpleSystemsManagement.class);
        amazonS3 = mock(AmazonS3.class);
    }

    @Test
    public void getConfigurationReturnConfigurationContentProperty() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest();
        final String configurationContent = "Hello world";
        final ResourceModel model = ResourceModel.builder().configurationContent(configurationContent).build();

        String result = handler.getConfiguration(model);
        assertEquals(configurationContent, result);
    }

    @Test
    public void getConfigurationReturnsDownloadedConfigurationFromUrlProperty() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest();
        final String configurationUrl = "http://www.mocky.io/v2/5dc19cab33000051e91a5437";
        final ResourceModel model = ResourceModel.builder().configurationUrl(configurationUrl).build();

        String expected = "Hello world";
        String result = handler.getConfiguration(model);

        assertEquals(expected, result);
    }

    @Test
    public void getConfigurationThrowsIfUrlDoesNotExistProperty() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest();
        final String configurationUrl = "http://acme.com/does/not/exist";
        final ResourceModel model = ResourceModel.builder().configurationUrl(configurationUrl).build();

        assertThrows(IllegalArgumentException.class, () -> {
            handler.getConfiguration(model);
        });
    }

    @Test
    public void getConfigurationReturnsDownloadedConfigurationFromS3PathProperty() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest(awsSimpleSystemsManagement, amazonS3);
        final String configurationS3Path = "s3://my-bucket/hello-world.txt";
        final ResourceModel model = ResourceModel.builder().configurationS3Path(configurationS3Path).build();

        final String expected = "Hello world";
        final S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new ByteArrayInputStream(expected.getBytes()));
        when(amazonS3.getObject(any())).thenReturn(s3Object);

        String result = handler.getConfiguration(model);

        assertEquals(expected, result);
    }

    @Test
    public void getConfigurationThrowsIfS3PathIsInvalid() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest();
        final String configurationS3Path = "http://acme.com/does/not/exist";
        final ResourceModel model = ResourceModel.builder().configurationS3Path(configurationS3Path).build();

        assertThrows(IllegalArgumentException.class, () -> {
            handler.getConfiguration(model);
        });
    }

    @Test
    public void getConfigurationThrowsIfS3PathDoesNotExistProperty() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest();
        final String configurationS3Path = "s3://bucket/does/not/exist";
        final ResourceModel model = ResourceModel.builder().configurationS3Path(configurationS3Path).build();

        assertThrows(IllegalArgumentException.class, () -> {
            handler.getConfiguration(model);
        });
    }

    @Test
    public void getConfigurationThrowsIfNoProperties() {
        final TerraformBaseHandlerUnderTest handler = new TerraformBaseHandlerUnderTest();
        final ResourceModel model = ResourceModel.builder().build();

        assertThrows(IllegalStateException.class, () -> {
            handler.getConfiguration(model);
        });
    }

    // TerraformBaseHandler is an abstract class. In order to tests methods within it, we need to create a dummy implementation
    class TerraformBaseHandlerUnderTest extends TerraformBaseHandler<CallbackContext> {
        public TerraformBaseHandlerUnderTest() {
            super();
        }

        public TerraformBaseHandlerUnderTest(AWSSimpleSystemsManagement awsSimpleSystemsManagement, AmazonS3 amazonS3) {
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

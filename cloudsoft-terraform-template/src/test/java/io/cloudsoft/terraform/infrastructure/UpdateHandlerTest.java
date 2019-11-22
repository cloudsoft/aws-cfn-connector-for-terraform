package io.cloudsoft.terraform.infrastructure;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import junit.framework.Assert;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private S3Client s3Client;

    @Mock
    private SsmClient ssmClient;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        TerraformBaseHandler.CREATE_NEW_DELEGATE_FOR_EACH_REQUEST = false;
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = new Logger() {
            @Override
            public void log(String arg) {
                System.out.println("LOG: " + arg);
            }
        };
    }

    @Test
    public void handleRequestCallWorkerRun() throws IOException {
        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final CallbackContext callbackContext = new CallbackContext();
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
        final UpdateHandler handler = new UpdateHandler();
        handler.setParameters(new TerraformParameters(ssmClient, s3Client));
        UpdateHandler spy = spy(handler);

        doReturn(progressEvent).when(spy).runStep();

        ProgressEvent<ResourceModel, CallbackContext> result = spy.handleRequest(proxy, request, callbackContext, logger);
        Assert.assertEquals(progressEvent, result);

        verify(spy, times(1)).runWithLoopingIfNecessary();
    }
}

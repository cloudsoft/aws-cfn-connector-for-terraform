package io.cloudsoft.terraform.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest {

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
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = new Logger() {
            @Override
            public void log(String arg) {
                System.out.println("LOG: " + arg);
            }
        };
    }

    @Test
    public void handleRequestCallWorkerRun() {
        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final CallbackContext callbackContext = new CallbackContext();
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
        final DeleteHandler handler = new DeleteHandler();
        handler.setParameters(new TerraformParameters(ssmClient, s3Client));
        DeleteHandler spy = spy(handler);

        doReturn(progressEvent).when(spy).run(any(), any());

        spy.handleRequest(proxy, request, callbackContext, logger);

        verify(spy, times(1)).run(eq(callbackContext), any());
    }
}

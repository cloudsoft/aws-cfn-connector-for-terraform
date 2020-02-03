package io.cloudsoft.terraform.infrastructure;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
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
public class HandlerTestFixture {

    @Mock
    protected AmazonWebServicesClientProxy proxy;

    @Mock
    protected S3Client s3Client;

    @Mock
    protected SsmClient ssmClient;

    protected Logger logger;

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

    public void doWorkerRun(Supplier<TerraformBaseHandler> handlerFactory) throws IOException {
        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final CallbackContext callbackContext = new CallbackContext();
        final ProgressEvent<ResourceModel, CallbackContext> progressEvent = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();

        // null value for all parameters
        final TerraformBaseWorker<?> worker = handlerFactory.get().newWorker();
        worker.storeMetadataOnServer = false;
        worker.setParameters(newTerraformParametersForTests(logger, proxy, ssmClient, s3Client));
        TerraformBaseWorker<?> spyWorker = spy(worker);

        final TerraformBaseHandler handler = handlerFactory.get();
        TerraformBaseHandler spyHandler = spy(handler);

        doReturn(progressEvent).when(spyWorker).runStep();
        doReturn(spyWorker).when(spyHandler).newWorker();

        ProgressEvent<ResourceModel, CallbackContext> result = spyHandler.handleRequest(proxy, request, callbackContext, logger);

        Assert.assertEquals(result, progressEvent);
        verify(spyWorker).runHandlingError();
    }

    static TerraformParameters newTerraformParametersForTests(Logger logger, AmazonWebServicesClientProxy proxy, SsmClient ssmClient, S3Client s3Client) {
        return new TerraformParameters(logger, proxy, ssmClient, s3Client) {
            // this one is read during init
            @Override
            public String getLogsS3BucketName() {
                return null;
            }
        };
    }

}

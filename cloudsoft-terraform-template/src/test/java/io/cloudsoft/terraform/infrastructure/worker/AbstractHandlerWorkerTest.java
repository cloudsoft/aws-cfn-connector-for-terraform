package io.cloudsoft.terraform.infrastructure.worker;

import io.cloudsoft.terraform.infrastructure.CallbackContext;
import io.cloudsoft.terraform.infrastructure.ResourceModel;
import io.cloudsoft.terraform.infrastructure.TerraformBaseHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AbstractHandlerWorkerTest {

    private enum NoSteps {}
    private static class EmptyHandlerWorker extends AbstractHandlerWorker<NoSteps> {
        EmptyHandlerWorker(AmazonWebServicesClientProxy proxy, ResourceHandlerRequest<ResourceModel> request,
            CallbackContext callbackContext, Logger logger, TerraformBaseHandler terraformBaseHandler) {
            super(proxy, request, callbackContext, logger, terraformBaseHandler);
        }

        @Override
        public ProgressEvent<ResourceModel, CallbackContext> doCall() {
            return null;
        }
    }
    
    @Mock
    private TerraformBaseHandler handler;

    @Mock
    private Logger logger;

    @Test
    public void throwsIfRequestIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new EmptyHandlerWorker(null, null, null, logger, handler));
    }

    @Test
    public void throwsIfRequestModelIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                    .build();
            new EmptyHandlerWorker(null, request, null, logger, handler);
        });
    }

    @Test
    public void initSuccessfully() {
        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final CallbackContext callbackContext = new CallbackContext();
        AbstractHandlerWorker<?> abstractHandlerWorker = new EmptyHandlerWorker(null, request, callbackContext, logger, handler);
        assertEquals(model, abstractHandlerWorker.model);
    }

    @Test
    public void logPrintsOutMessages() throws IOException {
        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final CallbackContext callbackContext = new CallbackContext();

        AbstractHandlerWorker<?> abstractHandlerWorker = new EmptyHandlerWorker(null, request, callbackContext, logger, handler);

        String message = "This is a message";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bo));

        abstractHandlerWorker.log(message);

        bo.flush();
        String allWrittenLines = new String(bo.toByteArray());

        assertTrue(allWrittenLines.contains(message));
        assertTrue(allWrittenLines.contains("<EOL>"));
        verify(logger).log(message);
    }
}

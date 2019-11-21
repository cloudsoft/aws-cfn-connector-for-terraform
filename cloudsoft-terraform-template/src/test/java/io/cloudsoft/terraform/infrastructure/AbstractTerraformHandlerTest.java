package io.cloudsoft.terraform.infrastructure;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

@ExtendWith(MockitoExtension.class)
public class AbstractTerraformHandlerTest {

    private enum NoSteps {}
    private static class EmptyHandler extends TerraformBaseHandler<NoSteps> {

        @Override
        public ProgressEvent<ResourceModel, CallbackContext> runStep() {
            return null;
        }

        // visible for testing
        @Override
        public void init(AmazonWebServicesClientProxy proxy, ResourceHandlerRequest<ResourceModel> request, CallbackContext callbackContext, Logger logger) {
            super.init(proxy, request, callbackContext, logger);
        }
        
        @Override
        public void log(String message) {
            super.log(message);
        }
    }
    
    @Mock
    private Logger logger;

    private EmptyHandler runEmptyHandler(AmazonWebServicesClientProxy proxy, ResourceHandlerRequest<ResourceModel> request, CallbackContext callbackContext, Logger logger) {
        EmptyHandler result = new EmptyHandler();
        result.init(proxy, request, callbackContext, logger);
        return result;
    }
    
    private EmptyHandler runEmptyHandler() {
        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final CallbackContext callbackContext = new CallbackContext();

        return runEmptyHandler(null, request, callbackContext, logger);
    }
    
    @Test
    public void throwsIfRequestIsNull() {
        assertThrows(NullPointerException.class, () -> new EmptyHandler().handleRequest(null, null, null, logger));
    }

    @Test
    public void initSuccessfully() {
        EmptyHandler h = runEmptyHandler();
        assertEquals(ResourceModel.builder().build(), h.model);
    }

    @Test
    public void cantInitTwice() {
        EmptyHandler h = runEmptyHandler();
        assertThrows(IllegalStateException.class, () -> h.init(null, null, null, logger));
    }

    @Test
    public void logPrintsOutMessages() throws IOException {
        EmptyHandler h = runEmptyHandler();

        String message = "This is a message";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bo));

        h.log(message);

        bo.flush();
        String allWrittenLines = new String(bo.toByteArray());

        assertTrue(allWrittenLines.contains(message));
        assertTrue(allWrittenLines.contains(TerraformBaseHandler.LOG_MESSAGE_SEPARATOR));
        verify(logger).log(message);
    }
}

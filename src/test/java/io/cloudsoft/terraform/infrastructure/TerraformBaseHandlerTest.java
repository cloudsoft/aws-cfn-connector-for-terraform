package io.cloudsoft.terraform.infrastructure;

import junit.framework.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TerraformBaseHandlerTest {

    private enum NoSteps {}
    
    public static class EmptyHandler extends TerraformBaseHandler {
        EmptyWorker w;
        @Override
        protected TerraformBaseWorker<?> newWorker() {
            return w = new EmptyWorker();
        }
    }
    
    public static class EmptyWorker extends TerraformBaseWorker<NoSteps> {
        @Override
        public ProgressEvent<ResourceModel, CallbackContext> runStep() {
            return progressEvents().success();
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
    Logger logger;
    
    private EmptyHandler lastHandler;

    private ProgressEvent<ResourceModel, CallbackContext> runEmptyHandler(AmazonWebServicesClientProxy proxy, ResourceHandlerRequest<ResourceModel> request, CallbackContext callbackContext, Logger logger) {
        EmptyHandler h = new EmptyHandler();
        this.lastHandler = h;
        return h.handleRequest(proxy, request, callbackContext, logger);
    }
    
    private ProgressEvent<ResourceModel, CallbackContext> runEmptyHandlerWithDefaults() {
        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final CallbackContext callbackContext = new CallbackContext();

        return runEmptyHandler(null, request, callbackContext, logger);
    }
    
    @Test
    public void failsIfRequestIsNull() {
        System.err.println("Expecting logged NPE to follow; ignore it");
        ProgressEvent<ResourceModel, CallbackContext> result = runEmptyHandler(null, null, null, logger);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getStatus(), OperationStatus.FAILED);
        String msg = result.getMessage();
        Assert.assertTrue("'"+msg+"' did not contain 'request'", msg.contains("request"));
        Assert.assertTrue("'"+msg+"' did not contain NPE", msg.contains(NullPointerException.class.getSimpleName()));
    }

    @Test
    public void initSuccessfully() {
        ProgressEvent<ResourceModel, CallbackContext> result = runEmptyHandlerWithDefaults();
        assertEquals(ResourceModel.builder().build(), result.getResourceModel());
    }

    @Test
    public void cantInitTwice() {
        runEmptyHandlerWithDefaults();
        assertThrows(IllegalStateException.class, () -> lastHandler.w.init(null, null, null, logger));
    }

    @Test
    public void logPrintsOutMessages() throws IOException {
        runEmptyHandlerWithDefaults();

        String message = "This is a message";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bo));

        lastHandler.w.log(message);

        bo.flush();
        String allWrittenLines = new String(bo.toByteArray());

        assertTrue(allWrittenLines.contains(message));
        assertTrue(allWrittenLines.contains(TerraformBaseWorker.LOG_MESSAGE_SEPARATOR));
        verify(logger).log(message);
    }
    
}

package io.cloudsoft.terraform.template.worker;

import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import io.cloudsoft.terraform.template.CallbackContext;
import io.cloudsoft.terraform.template.ResourceModel;
import io.cloudsoft.terraform.template.TerraformBaseHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static junit.framework.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AbstractHandlerWorkerTest {

    @Mock
    private TerraformBaseHandler<CallbackContext> handler;

    @Mock
    private Logger logger;

    @Test
    public void throwsIfRequestIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AbstractHandlerWorker(null, null, null, logger, handler) {
                @Override
                public ProgressEvent<ResourceModel, CallbackContext> call() {
                    return null;
                }
            };
        });
    }

    @Test
    public void throwsIfRequestModelIsNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                    .build();

            new AbstractHandlerWorker(null, request, null, logger, handler) {
                @Override
                public ProgressEvent<ResourceModel, CallbackContext> call() {
                    return null;
                }
            };
        });
    }

    @Test
    public void initSuccessfully() {
        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final CallbackContext callbackContext = new CallbackContext();

        AbstractHandlerWorker abstractHandlerWorker = new AbstractHandlerWorker(null, request, callbackContext, logger, handler) {
            @Override
            public ProgressEvent<ResourceModel, CallbackContext> call() {
                return null;
            }
        };

        assertEquals(model, abstractHandlerWorker.model);
        assertNotNull(abstractHandlerWorker.tfSync);
    }

    @Test
    public void logPrintsOutMessages() throws IOException {
        final ResourceModel model = ResourceModel.builder().build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();
        final CallbackContext callbackContext = new CallbackContext();

        AbstractHandlerWorker abstractHandlerWorker = new AbstractHandlerWorker(null, request, callbackContext, logger, handler) {
            @Override
            public ProgressEvent<ResourceModel, CallbackContext> call() {
                return null;
            }
        };

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

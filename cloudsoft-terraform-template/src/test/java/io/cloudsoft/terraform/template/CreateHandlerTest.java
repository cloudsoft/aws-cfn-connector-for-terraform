package io.cloudsoft.terraform.template;

import com.amazonaws.cloudformation.proxy.AmazonWebServicesClientProxy;
import com.amazonaws.cloudformation.proxy.Logger;
import com.amazonaws.cloudformation.proxy.OperationStatus;
import com.amazonaws.cloudformation.proxy.ProgressEvent;
import com.amazonaws.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private Logger logger;

    @BeforeEach
    public void setup() {
        proxy = mock(AmazonWebServicesClientProxy.class);
        logger = new Logger() {
            @Override
            public void log(String arg) {
                System.out.println("LOG: "+arg);
            }
        };
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateHandler handler = new CreateHandler();
        handler.asyncSshHelperFactory = cb -> new AsyncSshHelper(cb) {
            protected String exec(String command) {
                // don't execute, but include markers for pid and finished so that the rest of the class works
                return "TODO "+marker("PID")+" 999 "+marker("FINISHED")+" true";
            }
        };
        run(handler);
    }
    
    @Test
    public void handleRequest_OftenNotFinished() {
        final CreateHandler handler = new CreateHandler();
        handler.asyncSshHelperFactory = cb -> new AsyncSshHelper(cb) {
            protected String exec(String command) {
                // don't execute, but include markers for pid and finished so that the rest of the class works
                return "TODO "+marker("PID")+" 999 "+
                    (Math.random()<0.1 ? marker("FINISHED")+" true" : "not done yet");
            }
        };
        run(handler);
    }
    
    private void run(CreateHandler handler) {
        final ResourceModel model = ResourceModel.builder().build();
        model.setConfigurationContent("");

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        
        while (true) {
            logger.log("RESPONSE: "+response);
            assertThat(response).isNotNull();
            
            if (!response.isInProgress()) {
                break;
            }
            
            logger.log("CALLBACK: "+response.getCallbackContext());
            logger.log("DELAY: "+response.getCallbackDelaySeconds());
            assertThat(response.getCallbackContext()).isNotNull();
            
            response = handler.handleRequest(proxy, request, response.getCallbackContext(), logger);
        }
        
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }
    
    // junit misconfigured on eclipse, so workaround for IDE testing
    public static void main(String[] args) {
        CreateHandlerTest t = new CreateHandlerTest();
        t.setup();
        t.handleRequest_OftenNotFinished();
    }
}

package io.cloudsoft.terraform.infrastructure;

import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ssm.SsmClient;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends HandlerTestFixture {

    @Test
    public void handleRequestCallWorkerRun() throws IOException {
        doWorkerRun(() -> new CreateHandler());
    }
    
    public static void main(String[] args) throws IOException {
        CreateHandlerTest t = new CreateHandlerTest();
        t.s3Client = mock(S3Client.class);
        t.ssmClient = mock(SsmClient.class);
        
        t.setup();
        t.handleRequestCallWorkerRun();
    }
}

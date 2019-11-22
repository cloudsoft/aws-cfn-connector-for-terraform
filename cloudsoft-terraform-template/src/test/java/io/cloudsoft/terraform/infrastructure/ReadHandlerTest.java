package io.cloudsoft.terraform.infrastructure;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends HandlerTestFixture {

    @Test
    public void handleRequestCallWorkerRun() throws IOException {
        doWorkerRun(() -> new ReadHandler());
    }
    
}
package io.cloudsoft.terraform.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends HandlerTestFixture {

    @Test
    public void handleRequestCallWorkerRun() throws IOException {
        doWorkerRun(() -> new ReadHandler());
    }
    
}
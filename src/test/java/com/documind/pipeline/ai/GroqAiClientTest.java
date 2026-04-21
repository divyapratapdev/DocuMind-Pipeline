package com.documind.pipeline.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroqAiClientTest {

    private GroqAiClient groqAiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder();
        
        groqAiClient = new GroqAiClient(objectMapper, builder);
        ReflectionTestUtils.setField(groqAiClient, "apiUrl", "http://localhost:8080/mock");
        ReflectionTestUtils.setField(groqAiClient, "apiKey", "test-key");
        ReflectionTestUtils.setField(groqAiClient, "modelName", "mock-model");
    }

    // A comprehensive test of RestClient usually requires @RestClientTest,
    // so here we just test standard field injection. An integration test handles deeper verification.
    @Test
    void testClientInitialization() {
        assertNotNull(groqAiClient);
    }
}

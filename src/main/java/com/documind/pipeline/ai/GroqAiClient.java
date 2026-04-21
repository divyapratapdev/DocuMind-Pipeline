package com.documind.pipeline.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqAiClient {

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.model}")
    private String modelName;

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    
    public GroqAiClient(ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    public String extractContractDetails(String rawText) {
        log.info("Sending unstructured text chunk to Groq AI constraints engine...");

        if ("your_default_key_here".equals(apiKey) || apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("Using mock Groq AI Client due to missing GROQ_API_KEY. Bypassing HTTP call.");
            return "{\"governing_law\": \"Mock Law\", \"expiration_date\": \"2029-12-31\", \"high_risk\": true, \"risks\": [\"Mock Risk Detected\"]}";
        }

        String prompt = "You are a professional legal AI pipeline worker. " +
                "Extract the governing law, expiration date, and list any high-risk clauses from the following legal text. " +
                "YOU MUST RETURN STRICT JSON. Use the keys: 'governing_law' (string), 'expiration_date' (string, YYYY-MM-DD), 'high_risk' (boolean), and 'risks' (array of strings).\n\n" +
                "TEXT:\n" + rawText;

        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a legal data extraction engine. You only return raw JSON, no markdown syntax."),
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object"),
                "temperature", 0.1 // Lowest temperature for maximum determinism
        );

        try {
            String response = restClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(requestBody))
                    .retrieve()
                    .body(String.class);

            // Parse response from Groq OpenAI spec format:
            // {"choices": [{"message": {"content": "{...}"}}]}
            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            
            log.info("Successfully extracted structured JSON from LLM: {}", content);
            return content;
            
        } catch (Exception e) {
            log.error("AI extraction failed: {}", e.getMessage());
            throw new RuntimeException("LLM integration error", e);
        }
    }
}

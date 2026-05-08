package com.Email.Assistant;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Value("${API_URL}")
    private String apiUrl;

    @Value("${API_KEY}")
    private String apiKey;
    
    public String generateEmailReply(EmailRequest request) {

        //Build Prompt
        String prompt = BuildPrompt(request);


        //Craft a Request
        Map<String, Object> apiRequestBody = new HashMap<>();
        apiRequestBody.put("model", "llama-3.3-70b-versatile");
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        apiRequestBody.put("messages", Arrays.asList(message));


        //Do request and get response
        String response = webClient.post()
            .uri(apiUrl)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .bodyValue(apiRequestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block();

        //Extract and return the generated email content
        return ExtractContentFromResponse(response);
    }

    @SuppressWarnings("deprecation")
    private String ExtractContentFromResponse(String response) {
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response);
            return rootNode.path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
        }
        catch(Exception ex) {
            return "Error processing response : " + ex.getMessage();
        }
    }

    private String BuildPrompt(EmailRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate an email reply for the following email. Please don't generate any subject line");
        if(request.getTone() != null && !request.getTone().isEmpty()) {
            prompt.append(" with a ").append(request.getTone()).append(" tone.");
        }
        else{
            prompt.append(" with a Professional tone");
        }
        prompt.append("\n\nOriginal Email Content:\n").append(request.getEmailContent());
        return prompt.toString();
    }
}

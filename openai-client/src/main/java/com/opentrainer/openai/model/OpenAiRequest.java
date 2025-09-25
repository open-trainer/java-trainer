package com.opentrainer.openai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request model for OpenAI API
 */
public record OpenAiRequest(
    @JsonProperty("model")
    String model,
    
    @JsonProperty("messages")
    List<Message> messages,
    
    @JsonProperty("max_tokens")
    Integer maxTokens,
    
    @JsonProperty("temperature")
    Double temperature,
    
    @JsonProperty("response_format")
    ResponseFormat responseFormat
) {
    
    public record Message(
        @JsonProperty("role")
        String role,
        
        @JsonProperty("content")
        String content
    ) {}
    
    public record ResponseFormat(
        @JsonProperty("type")
        String type
    ) {}
    
    public static OpenAiRequest createTrainingPlanRequest(String prompt) {
        return new OpenAiRequest(
            "gpt-4-turbo-preview",
            List.of(new Message("user", prompt)),
            4000,
            0.7,
            new ResponseFormat("json_object")
        );
    }
}
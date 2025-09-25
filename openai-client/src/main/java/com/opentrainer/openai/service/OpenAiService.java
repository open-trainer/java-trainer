package com.opentrainer.openai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentrainer.openai.model.OpenAiRequest;
import com.opentrainer.openai.model.OpenAiResponse;
import com.opentrainer.openai.model.TrainingPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

@Service
public class OpenAiService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public OpenAiService(@Value("${openai.api.key}") String apiKey,
                        @Value("${openai.api.base-url:https://api.openai.com/v1}") String baseUrl,
                        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
    
    /**
     * Generates a training plan using OpenAI API
     */
    public Mono<TrainingPlan> generateTrainingPlan(String prompt, String userId) {
        logger.info("Generating training plan for user: {}", userId);
        
        OpenAiRequest request = OpenAiRequest.createTrainingPlanRequest(prompt);
        
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof WebClientException))
                .map(response -> parseTrainingPlan(response, userId))
                .doOnSuccess(plan -> logger.info("Successfully generated training plan for user: {}", userId))
                .doOnError(error -> logger.error("Failed to generate training plan for user: {}", userId, error));
    }
    
    /**
     * Builds a prompt from health message data
     */
    public String buildPrompt(String userId, String healthData) {
        return String.format("""
            Create a personalized training plan based on the following health data:
            
            %s
            
            Please provide a comprehensive training plan in JSON format with the following structure:
            {
                "plan_id": "unique_id",
                "user_id": "%s",
                "title": "Training Plan Title",
                "description": "Brief description of the plan",
                "duration_weeks": 12,
                "difficulty_level": "beginner|intermediate|advanced",
                "weekly_schedule": [
                    {
                        "week": 1,
                        "workouts": [
                            {
                                "day": "Monday",
                                "type": "strength|cardio|flexibility",
                                "duration_minutes": 45,
                                "exercises": [
                                    {
                                        "name": "Exercise name",
                                        "sets": 3,
                                        "reps": "8-12",
                                        "weight": "bodyweight or specific weight",
                                        "instructions": "Clear instructions"
                                    }
                                ],
                                "rest_periods": "60-90 seconds between sets"
                            }
                        ]
                    }
                ],
                "nutrition_guidelines": "Basic nutrition advice",
                "safety_considerations": ["Important safety notes"]
            }
            
            Consider the user's current fitness level, medical conditions, goals, and preferences.
            Ensure the plan is progressive, safe, and achievable.
            """, healthData, userId);
    }
    
    private TrainingPlan parseTrainingPlan(OpenAiResponse response, String userId) {
        try {
            if (response.choices().isEmpty()) {
                throw new RuntimeException("No choices returned from OpenAI API");
            }
            
            String content = response.choices().get(0).message().content();
            TrainingPlan plan = objectMapper.readValue(content, TrainingPlan.class);
            
            // Ensure plan has user ID and generated plan ID
            return new TrainingPlan(
                plan.planId() != null ? plan.planId() : UUID.randomUUID().toString(),
                userId,
                plan.title(),
                plan.description(),
                plan.durationWeeks(),
                plan.difficultyLevel(),
                plan.weeklySchedule(),
                plan.nutritionGuidelines(),
                plan.safetyConsiderations()
            );
        } catch (Exception e) {
            logger.error("Failed to parse training plan from OpenAI response", e);
            throw new RuntimeException("Failed to parse training plan from OpenAI response", e);
        }
    }
}
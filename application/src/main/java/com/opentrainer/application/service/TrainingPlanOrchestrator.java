package com.opentrainer.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentrainer.dynamodb.model.TrainingMetadata;
import com.opentrainer.dynamodb.repository.TrainingMetadataRepository;
import com.opentrainer.notifier.service.NotificationService;
import com.opentrainer.openai.model.TrainingPlan;
import com.opentrainer.openai.service.OpenAiService;
import com.opentrainer.sqsconsumer.model.HealthMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Orchestrates the entire training plan generation process
 */
@Service
public class TrainingPlanOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(TrainingPlanOrchestrator.class);
    
    private final OpenAiService openAiService;
    private final TrainingMetadataRepository trainingMetadataRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    
    public TrainingPlanOrchestrator(OpenAiService openAiService,
                                  TrainingMetadataRepository trainingMetadataRepository,
                                  NotificationService notificationService,
                                  ObjectMapper objectMapper) {
        this.openAiService = openAiService;
        this.trainingMetadataRepository = trainingMetadataRepository;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Processes health message and generates training plan
     */
    public void processHealthMessage(HealthMessage healthMessage) {
        logger.info("Processing health message for user: {}", healthMessage.userId());
        
        try {
            // Create initial metadata record
            TrainingMetadata initialMetadata = createInitialMetadata(healthMessage);
            trainingMetadataRepository.save(initialMetadata);
            
            // Build prompt from health data
            String healthData = buildHealthDataString(healthMessage);
            String prompt = openAiService.buildPrompt(healthMessage.userId(), healthData);
            
            // Generate training plan asynchronously
            openAiService.generateTrainingPlan(prompt, healthMessage.userId())
                    .subscribe(
                            trainingPlan -> handleTrainingPlanGenerated(trainingPlan, healthMessage.userId()),
                            error -> handleTrainingPlanError(error, healthMessage.userId())
                    );
            
        } catch (Exception e) {
            logger.error("Error processing health message for user: {}", healthMessage.userId(), e);
            handleTrainingPlanError(e, healthMessage.userId());
        }
    }
    
    /**
     * Handles successful training plan generation
     */
    private void handleTrainingPlanGenerated(TrainingPlan trainingPlan, String userId) {
        try {
            logger.info("Training plan generated successfully for user: {}", userId);
            
            // Update metadata with training plan details
            TrainingMetadata metadata = new TrainingMetadata(
                    userId,
                    trainingPlan.planId(),
                    trainingPlan.title(),
                    trainingPlan.description(),
                    trainingPlan.durationWeeks(),
                    trainingPlan.difficultyLevel(),
                    "GENERATED",
                    Instant.now(),
                    Instant.now(),
                    createAdditionalMetadata(trainingPlan)
            );
            
            trainingMetadataRepository.save(metadata);
            
            // Send success notification
            notificationService.sendTrainingPlanGeneratedNotification(
                    userId, trainingPlan.planId(), trainingPlan.title()
            );
            
            logger.info("Successfully processed training plan for user: {} with plan ID: {}", 
                       userId, trainingPlan.planId());
        } catch (Exception e) {
            logger.error("Error handling training plan generation for user: {}", userId, e);
            handleTrainingPlanError(e, userId);
        }
    }
    
    /**
     * Handles training plan generation error
     */
    private void handleTrainingPlanError(Throwable error, String userId) {
        try {
            logger.error("Training plan generation failed for user: {}", userId, error);
            
            // Update metadata status to ERROR
            trainingMetadataRepository.findByUserId(userId)
                    .stream()
                    .filter(metadata -> "PROCESSING".equals(metadata.getStatus()))
                    .findFirst()
                    .ifPresent(metadata -> {
                        trainingMetadataRepository.updateStatus(userId, metadata.getPlanId(), "ERROR");
                    });
            
            // Send error notification
            notificationService.sendTrainingPlanErrorNotification(userId, error.getMessage());
            
        } catch (Exception e) {
            logger.error("Error handling training plan failure for user: {}", userId, e);
        }
    }
    
    /**
     * Creates initial metadata record for processing
     */
    private TrainingMetadata createInitialMetadata(HealthMessage healthMessage) {
        return new TrainingMetadata(
                healthMessage.userId(),
                "PROCESSING-" + System.currentTimeMillis(),
                "Training Plan (Processing)",
                "Training plan is being generated based on your health data",
                null,
                null,
                "PROCESSING",
                Instant.now(),
                Instant.now(),
                Map.of(
                        "fitness_level", healthMessage.currentFitnessLevel(),
                        "goals_count", String.valueOf(healthMessage.goals().size())
                )
        );
    }
    
    /**
     * Builds health data string for OpenAI prompt
     */
    private String buildHealthDataString(HealthMessage healthMessage) {
        try {
            return objectMapper.writeValueAsString(healthMessage);
        } catch (Exception e) {
            logger.warn("Failed to serialize health message, using fallback", e);
            return String.format("""
                User ID: %s
                Current Fitness Level: %s
                Goals: %s
                Age: %d
                Weight: %.1f
                Height: %.1f
                Activity Level: %s
                Medical Conditions: %s
                """, 
                healthMessage.userId(),
                healthMessage.currentFitnessLevel(),
                String.join(", ", healthMessage.goals()),
                healthMessage.healthMetrics().age(),
                healthMessage.healthMetrics().weight(),
                healthMessage.healthMetrics().height(),
                healthMessage.healthMetrics().activityLevel(),
                String.join(", ", healthMessage.healthMetrics().medicalConditions())
            );
        }
    }
    
    /**
     * Creates additional metadata map from training plan
     */
    private Map<String, String> createAdditionalMetadata(TrainingPlan trainingPlan) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("weekly_schedule_count", String.valueOf(trainingPlan.weeklySchedule().size()));
        metadata.put("safety_considerations_count", String.valueOf(trainingPlan.safetyConsiderations().size()));
        
        if (trainingPlan.nutritionGuidelines() != null) {
            metadata.put("has_nutrition_guidelines", "true");
        }
        
        return metadata;
    }
}
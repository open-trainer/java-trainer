package com.opentrainer.sqsconsumer.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents a health message consumed from SQS queue
 */
public record HealthMessage(
    @JsonProperty("user_id")
    String userId,
    
    @JsonProperty("timestamp")
    LocalDateTime timestamp,
    
    @JsonProperty("health_metrics")
    HealthMetrics healthMetrics,
    
    @JsonProperty("goals")
    List<String> goals,
    
    @JsonProperty("preferences")
    Map<String, Object> preferences,
    
    @JsonProperty("current_fitness_level")
    String currentFitnessLevel
) {
    
    public record HealthMetrics(
        @JsonProperty("age")
        Integer age,
        
        @JsonProperty("weight")
        Double weight,
        
        @JsonProperty("height")
        Double height,
        
        @JsonProperty("heart_rate")
        Integer heartRate,
        
        @JsonProperty("blood_pressure")
        String bloodPressure,
        
        @JsonProperty("activity_level")
        String activityLevel,
        
        @JsonProperty("medical_conditions")
        List<String> medicalConditions
    ) {}
}
package com.opentrainer.notifier.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a notification message to be sent to SQS
 */
public record NotificationMessage(
    @JsonProperty("user_id")
    String userId,
    
    @JsonProperty("plan_id")
    String planId,
    
    @JsonProperty("notification_type")
    String notificationType,
    
    @JsonProperty("title")
    String title,
    
    @JsonProperty("message")
    String message,
    
    @JsonProperty("timestamp")
    LocalDateTime timestamp,
    
    @JsonProperty("priority")
    String priority,
    
    @JsonProperty("metadata")
    Map<String, Object> metadata
) {
    
    public static NotificationMessage trainingPlanGenerated(String userId, String planId, String planTitle) {
        return new NotificationMessage(
            userId,
            planId,
            "TRAINING_PLAN_GENERATED",
            "Your Training Plan is Ready!",
            String.format("Your personalized training plan '%s' has been generated and is ready for you to start.", planTitle),
            LocalDateTime.now(),
            "HIGH",
            Map.of(
                "action_required", "VIEW_PLAN",
                "plan_title", planTitle
            )
        );
    }
    
    public static NotificationMessage trainingPlanError(String userId, String errorMessage) {
        return new NotificationMessage(
            userId,
            null,
            "TRAINING_PLAN_ERROR",
            "Training Plan Generation Failed",
            String.format("We encountered an issue generating your training plan: %s. Please try again or contact support.", errorMessage),
            LocalDateTime.now(),
            "MEDIUM",
            Map.of(
                "error_type", "GENERATION_FAILED",
                "requires_retry", "true"
            )
        );
    }
}
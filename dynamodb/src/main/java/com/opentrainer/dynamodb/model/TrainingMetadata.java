package com.opentrainer.dynamodb.model;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.Map;

/**
 * DynamoDB entity for storing training plan metadata
 */
@DynamoDbBean
public class TrainingMetadata {
    
    private String userId;
    private String planId;
    private String title;
    private String description;
    private Integer durationWeeks;
    private String difficultyLevel;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private Map<String, String> additionalMetadata;
    
    public TrainingMetadata() {}
    
    public TrainingMetadata(String userId, String planId, String title, String description,
                           Integer durationWeeks, String difficultyLevel, String status,
                           Instant createdAt, Instant updatedAt, Map<String, String> additionalMetadata) {
        this.userId = userId;
        this.planId = planId;
        this.title = title;
        this.description = description;
        this.durationWeeks = durationWeeks;
        this.difficultyLevel = difficultyLevel;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.additionalMetadata = additionalMetadata;
    }
    
    @DynamoDbPartitionKey
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    @DynamoDbSortKey
    public String getPlanId() {
        return planId;
    }
    
    public void setPlanId(String planId) {
        this.planId = planId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Integer getDurationWeeks() {
        return durationWeeks;
    }
    
    public void setDurationWeeks(Integer durationWeeks) {
        this.durationWeeks = durationWeeks;
    }
    
    public String getDifficultyLevel() {
        return difficultyLevel;
    }
    
    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Map<String, String> getAdditionalMetadata() {
        return additionalMetadata;
    }
    
    public void setAdditionalMetadata(Map<String, String> additionalMetadata) {
        this.additionalMetadata = additionalMetadata;
    }
}
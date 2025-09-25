package com.opentrainer.dynamodb.repository;

import com.opentrainer.dynamodb.model.TrainingMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class TrainingMetadataRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(TrainingMetadataRepository.class);
    
    private final DynamoDbTable<TrainingMetadata> table;
    
    public TrainingMetadataRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("training-metadata", TableSchema.fromBean(TrainingMetadata.class));
    }
    
    /**
     * Saves training metadata to DynamoDB
     */
    public TrainingMetadata save(TrainingMetadata metadata) {
        try {
            metadata.setUpdatedAt(Instant.now());
            if (metadata.getCreatedAt() == null) {
                metadata.setCreatedAt(Instant.now());
            }
            
            table.putItem(metadata);
            logger.info("Successfully saved training metadata for user: {} and plan: {}", 
                       metadata.getUserId(), metadata.getPlanId());
            return metadata;
        } catch (Exception e) {
            logger.error("Failed to save training metadata for user: {} and plan: {}", 
                        metadata.getUserId(), metadata.getPlanId(), e);
            throw new RuntimeException("Failed to save training metadata", e);
        }
    }
    
    /**
     * Finds training metadata by user ID and plan ID
     */
    public Optional<TrainingMetadata> findByUserIdAndPlanId(String userId, String planId) {
        try {
            Key key = Key.builder()
                    .partitionValue(userId)
                    .sortValue(planId)
                    .build();
            
            TrainingMetadata metadata = table.getItem(key);
            return Optional.ofNullable(metadata);
        } catch (Exception e) {
            logger.error("Failed to find training metadata for user: {} and plan: {}", userId, planId, e);
            return Optional.empty();
        }
    }
    
    /**
     * Finds all training metadata for a user
     */
    public List<TrainingMetadata> findByUserId(String userId) {
        try {
            QueryConditional queryConditional = QueryConditional.keyEqualTo(
                    Key.builder().partitionValue(userId).build()
            );
            
            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                    .queryConditional(queryConditional)
                    .build();
            
            return table.query(queryRequest)
                    .items()
                    .stream()
                    .toList();
        } catch (Exception e) {
            logger.error("Failed to find training metadata for user: {}", userId, e);
            throw new RuntimeException("Failed to find training metadata for user: " + userId, e);
        }
    }
    
    /**
     * Updates training metadata status
     */
    public void updateStatus(String userId, String planId, String status) {
        try {
            Optional<TrainingMetadata> existingMetadata = findByUserIdAndPlanId(userId, planId);
            
            if (existingMetadata.isPresent()) {
                TrainingMetadata metadata = existingMetadata.get();
                metadata.setStatus(status);
                save(metadata);
                logger.info("Updated status to {} for user: {} and plan: {}", status, userId, planId);
            } else {
                logger.warn("Training metadata not found for user: {} and plan: {}", userId, planId);
            }
        } catch (Exception e) {
            logger.error("Failed to update status for user: {} and plan: {}", userId, planId, e);
            throw new RuntimeException("Failed to update training metadata status", e);
        }
    }
    
    /**
     * Deletes training metadata
     */
    public void delete(String userId, String planId) {
        try {
            Key key = Key.builder()
                    .partitionValue(userId)
                    .sortValue(planId)
                    .build();
            
            table.deleteItem(key);
            logger.info("Deleted training metadata for user: {} and plan: {}", userId, planId);
        } catch (Exception e) {
            logger.error("Failed to delete training metadata for user: {} and plan: {}", userId, planId, e);
            throw new RuntimeException("Failed to delete training metadata", e);
        }
    }
}
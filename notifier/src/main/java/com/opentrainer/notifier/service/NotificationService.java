package com.opentrainer.notifier.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentrainer.notifier.model.NotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Map;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String notificationQueueUrl;
    
    public NotificationService(@Qualifier("notifierSqsClient") SqsClient sqsClient,
                              ObjectMapper objectMapper,
                              @Value("${aws.sqs.notification-queue-url}") String notificationQueueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.notificationQueueUrl = notificationQueueUrl;
    }
    
    /**
     * Sends notification message to SQS queue
     */
    public void sendNotification(NotificationMessage notification) {
        try {
            String messageBody = objectMapper.writeValueAsString(notification);
            
            SendMessageRequest request = SendMessageRequest.builder()
                    .queueUrl(notificationQueueUrl)
                    .messageBody(messageBody)
                    .messageAttributes(buildMessageAttributes(notification))
                    .build();
            
            SendMessageResponse response = sqsClient.sendMessage(request);
            
            logger.info("Successfully sent notification to SQS. MessageId: {}, UserId: {}, Type: {}", 
                       response.messageId(), notification.userId(), notification.notificationType());
        } catch (Exception e) {
            logger.error("Failed to send notification for user: {} and type: {}", 
                        notification.userId(), notification.notificationType(), e);
            throw new RuntimeException("Failed to send notification", e);
        }
    }
    
    /**
     * Sends training plan generated notification
     */
    public void sendTrainingPlanGeneratedNotification(String userId, String planId, String planTitle) {
        NotificationMessage notification = NotificationMessage.trainingPlanGenerated(userId, planId, planTitle);
        sendNotification(notification);
    }
    
    /**
     * Sends training plan error notification
     */
    public void sendTrainingPlanErrorNotification(String userId, String errorMessage) {
        NotificationMessage notification = NotificationMessage.trainingPlanError(userId, errorMessage);
        sendNotification(notification);
    }
    
    /**
     * Builds message attributes for SQS message
     */
    private Map<String, MessageAttributeValue> buildMessageAttributes(NotificationMessage notification) {
        return Map.of(
            "userId", MessageAttributeValue.builder()
                    .stringValue(notification.userId())
                    .dataType("String")
                    .build(),
            "notificationType", MessageAttributeValue.builder()
                    .stringValue(notification.notificationType())
                    .dataType("String")
                    .build(),
            "priority", MessageAttributeValue.builder()
                    .stringValue(notification.priority())
                    .dataType("String")
                    .build()
        );
    }
}
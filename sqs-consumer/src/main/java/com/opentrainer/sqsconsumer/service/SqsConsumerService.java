package com.opentrainer.sqsconsumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentrainer.sqsconsumer.model.HealthMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class SqsConsumerService {
    
    private static final Logger logger = LoggerFactory.getLogger(SqsConsumerService.class);
    
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;
    
    public SqsConsumerService(@Qualifier("sqsClient") SqsClient sqsClient, 
                             ObjectMapper objectMapper,
                             @Value("${aws.sqs.health-queue-url}") String queueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.queueUrl = queueUrl;
    }
    
    /**
     * Polls messages from SQS queue and processes them
     */
    public void pollMessages(Consumer<HealthMessage> messageProcessor) {
        try {
            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(20) // Long polling
                    .visibilityTimeout(30)
                    .build();
            
            ReceiveMessageResponse response = sqsClient.receiveMessage(request);
            List<Message> messages = response.messages();
            
            logger.info("Received {} messages from SQS queue", messages.size());
            
            for (Message message : messages) {
                try {
                    Optional<HealthMessage> healthMessage = parseMessage(message.body());
                    
                    if (healthMessage.isPresent()) {
                        messageProcessor.accept(healthMessage.get());
                        deleteMessage(message.receiptHandle());
                        logger.info("Successfully processed message for user: {}", 
                                  healthMessage.get().userId());
                    } else {
                        logger.warn("Failed to parse message, moving to DLQ: {}", message.body());
                        deleteMessage(message.receiptHandle()); // Remove invalid message
                    }
                } catch (Exception e) {
                    logger.error("Error processing message: {}", message.body(), e);
                    // Message will remain in queue due to visibility timeout
                }
            }
        } catch (Exception e) {
            logger.error("Error polling messages from SQS", e);
            throw new RuntimeException("Failed to poll messages from SQS", e);
        }
    }
    
    /**
     * Parses SQS message body to HealthMessage
     */
    private Optional<HealthMessage> parseMessage(String messageBody) {
        try {
            HealthMessage healthMessage = objectMapper.readValue(messageBody, HealthMessage.class);
            return Optional.of(healthMessage);
        } catch (Exception e) {
            logger.error("Failed to parse message body: {}", messageBody, e);
            return Optional.empty();
        }
    }
    
    /**
     * Deletes processed message from SQS queue
     */
    private void deleteMessage(String receiptHandle) {
        try {
            DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(receiptHandle)
                    .build();
            
            sqsClient.deleteMessage(deleteRequest);
        } catch (Exception e) {
            logger.error("Failed to delete message with receipt handle: {}", receiptHandle, e);
        }
    }
}
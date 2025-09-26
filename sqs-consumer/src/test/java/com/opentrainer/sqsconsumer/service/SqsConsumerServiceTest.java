package com.opentrainer.sqsconsumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentrainer.sqsconsumer.model.HealthMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqsConsumerServiceTest {

    @Mock
    private SqsClient sqsClient;

    private ObjectMapper objectMapper;
    private SqsConsumerService sqsConsumerService;
    private final String queueUrl = "http://localhost:4566/000000000000/test-health-queue";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sqsConsumerService = new SqsConsumerService(sqsClient, objectMapper, queueUrl);
    }

    @Test
    void testPollMessages_ProcessesValidMessage() throws Exception {
        // Given - using simple string message format for test
        String messageBody = """
            {
              "user_id": "user123",
              "timestamp": "2024-01-15T10:30:00",
              "health_metrics": {
                "age": 30,
                "weight": 70.5,
                "height": 175.0,
                "heart_rate": 72,
                "blood_pressure": "120/80",
                "activity_level": "moderate",
                "medical_conditions": ["none"]
              },
              "goals": ["weight_loss", "muscle_gain"],
              "preferences": {"workout_type": "strength_training"},
              "current_fitness_level": "intermediate"
            }
            """;
        
        Message sqsMessage = Message.builder()
                .body(messageBody)
                .receiptHandle("receipt-handle-123")
                .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(List.of(sqsMessage))
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(response);

        AtomicReference<HealthMessage> processedMessage = new AtomicReference<>();

        // When
        sqsConsumerService.pollMessages(processedMessage::set);

        // Then
        assertNotNull(processedMessage.get());
        assertEquals("user123", processedMessage.get().userId());
        assertEquals("intermediate", processedMessage.get().currentFitnessLevel());
        
        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void testPollMessages_HandlesEmptyResponse() {
        // Given
        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(List.of())
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(response);

        AtomicReference<HealthMessage> processedMessage = new AtomicReference<>();

        // When
        sqsConsumerService.pollMessages(processedMessage::set);

        // Then
        assertNull(processedMessage.get());
        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void testPollMessages_HandlesInvalidMessage() {
        // Given
        Message sqsMessage = Message.builder()
                .body("{invalid json}")
                .receiptHandle("receipt-handle-123")
                .build();

        ReceiveMessageResponse response = ReceiveMessageResponse.builder()
                .messages(List.of(sqsMessage))
                .build();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(response);

        AtomicReference<HealthMessage> processedMessage = new AtomicReference<>();

        // When
        sqsConsumerService.pollMessages(processedMessage::set);

        // Then
        assertNull(processedMessage.get());
        verify(sqsClient).receiveMessage(any(ReceiveMessageRequest.class));
        verify(sqsClient).deleteMessage(any(DeleteMessageRequest.class)); // Invalid messages are deleted
    }
}
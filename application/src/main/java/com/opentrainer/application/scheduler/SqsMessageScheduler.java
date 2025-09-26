package com.opentrainer.application.scheduler;

import com.opentrainer.application.service.TrainingPlanOrchestrator;
import com.opentrainer.sqsconsumer.service.SqsConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for polling SQS messages and processing them
 */
@Component
@ConditionalOnProperty(value = "app.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SqsMessageScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(SqsMessageScheduler.class);
    
    private final SqsConsumerService sqsConsumerService;
    private final TrainingPlanOrchestrator trainingPlanOrchestrator;
    
    public SqsMessageScheduler(SqsConsumerService sqsConsumerService,
                              TrainingPlanOrchestrator trainingPlanOrchestrator) {
        this.sqsConsumerService = sqsConsumerService;
        this.trainingPlanOrchestrator = trainingPlanOrchestrator;
    }
    
    /**
     * Polls SQS messages every 30 seconds
     */
    @Scheduled(fixedDelayString = "${app.scheduler.sqs.poll-interval:30000}")
    public void pollSqsMessages() {
        logger.debug("Polling SQS messages for health data...");
        
        try {
            sqsConsumerService.pollMessages(trainingPlanOrchestrator::processHealthMessage);
        } catch (Exception e) {
            logger.error("Error occurred while polling SQS messages", e);
        }
    }
}
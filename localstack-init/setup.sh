#!/bin/bash

# Wait for LocalStack to be ready
echo "Waiting for LocalStack to be ready..."
sleep 10

# Create SQS queues
echo "Creating SQS queues..."
awslocal sqs create-queue --queue-name health-queue --region us-east-1
awslocal sqs create-queue --queue-name notification-queue --region us-east-1

# Create DynamoDB table
echo "Creating DynamoDB table..."
awslocal dynamodb create-table \
    --table-name training-metadata \
    --attribute-definitions \
        AttributeName=userId,AttributeType=S \
        AttributeName=planId,AttributeType=S \
    --key-schema \
        AttributeName=userId,KeyType=HASH \
        AttributeName=planId,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --region us-east-1

# Verify resources
echo "Verifying created resources..."
awslocal sqs list-queues --region us-east-1
awslocal dynamodb list-tables --region us-east-1

echo "LocalStack setup complete!"
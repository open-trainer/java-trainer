# Deployment Guide

This guide covers deploying the Java Trainer application to various environments.

## Prerequisites

- AWS CLI configured with appropriate credentials
- Docker installed
- kubectl configured (for Kubernetes deployment)

## Local Development

### Using Docker Compose

1. Copy environment configuration:
```bash
cp .env.example .env
```

2. Update `.env` with your values:
```bash
OPENAI_API_KEY=your-api-key-here
```

3. Start the application:
```bash
docker-compose up -d
```

4. Check health:
```bash
curl http://localhost:8080/actuator/health
```

## AWS ECS Deployment

### 1. Setup Infrastructure

Create required AWS resources:

```bash
# Create ECR repository
aws ecr create-repository --repository-name java-trainer

# Create ECS cluster
aws ecs create-cluster --cluster-name java-trainer-cluster

# Create log groups
aws logs create-log-group --log-group-name /ecs/java-trainer
aws logs create-log-group --log-group-name /ecs/java-trainer-staging
```

### 2. Build and Push Docker Image

```bash
# Get ECR login token
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com

# Build and tag image
docker build -t java-trainer .
docker tag java-trainer:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/java-trainer:latest

# Push image
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/java-trainer:latest
```

### 3. Update Task Definition

Update the task definition files in `.aws/` directory with your AWS account ID and resource ARNs.

### 4. Deploy Service

```bash
# Register task definition
aws ecs register-task-definition --cli-input-json file://.aws/task-definition-production.json

# Create or update service
aws ecs create-service \
  --cluster java-trainer-cluster \
  --service-name java-trainer-service \
  --task-definition java-trainer-task-definition \
  --desired-count 2
```

## AWS Secrets Manager Setup

Store sensitive configuration in AWS Secrets Manager:

```bash
# Store OpenAI API key
aws secretsmanager create-secret \
  --name openai-api-key \
  --secret-string "your-openai-api-key"

# Store SQS URLs
aws secretsmanager create-secret \
  --name sqs-health-queue-url \
  --secret-string "https://sqs.us-east-1.amazonaws.com/123456789012/health-queue"

aws secretsmanager create-secret \
  --name sqs-notification-queue-url \
  --secret-string "https://sqs.us-east-1.amazonaws.com/123456789012/notification-queue"
```

## Kubernetes Deployment

### 1. Create Namespace

```bash
kubectl create namespace java-trainer
```

### 2. Create ConfigMap and Secrets

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: java-trainer-config
  namespace: java-trainer
data:
  AWS_REGION: "us-east-1"
  SPRING_PROFILES_ACTIVE: "production"
```

```yaml
# secrets.yaml
apiVersion: v1
kind: Secret
metadata:
  name: java-trainer-secrets
  namespace: java-trainer
type: Opaque
stringData:
  OPENAI_API_KEY: "your-openai-api-key"
  AWS_SQS_HEALTH_QUEUE_URL: "your-health-queue-url"
  AWS_SQS_NOTIFICATION_QUEUE_URL: "your-notification-queue-url"
```

### 3. Deploy Application

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: java-trainer
  namespace: java-trainer
spec:
  replicas: 2
  selector:
    matchLabels:
      app: java-trainer
  template:
    metadata:
      labels:
        app: java-trainer
    spec:
      containers:
      - name: java-trainer
        image: <account-id>.dkr.ecr.us-east-1.amazonaws.com/java-trainer:latest
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: java-trainer-config
        - secretRef:
            name: java-trainer-secrets
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 60
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
```

```bash
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml
kubectl apply -f deployment.yaml
```

## Monitoring Setup

### Prometheus Configuration

The application exposes metrics at `/actuator/prometheus`. Configure Prometheus to scrape these metrics:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'java-trainer'
    static_configs:
      - targets: ['java-trainer:8080']
    metrics_path: '/actuator/prometheus'
```

### Grafana Dashboards

Import the provided Grafana dashboard from `monitoring/grafana/dashboards/` to visualize application metrics.

## Health Checks and Monitoring

The application provides several endpoints for monitoring:

- `/actuator/health` - Application health status
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus formatted metrics
- `/actuator/info` - Application information

## Troubleshooting

### Common Issues

1. **Application won't start**: Check environment variables and AWS credentials
2. **SQS connection issues**: Verify queue URLs and IAM permissions
3. **OpenAI API errors**: Check API key and rate limits
4. **DynamoDB errors**: Verify table exists and IAM permissions

### Logs

Check application logs:

```bash
# Docker Compose
docker-compose logs java-trainer

# ECS
aws logs tail /ecs/java-trainer --follow

# Kubernetes
kubectl logs -f deployment/java-trainer -n java-trainer
```
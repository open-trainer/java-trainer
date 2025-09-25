# Java Trainer - AI-Powered Training Plan Generator

A Java microservice that consumes health messages from Amazon SQS, generates personalized training plans using OpenAI, stores metadata in DynamoDB, and publishes notifications to SQS.

## Architecture

The application follows a modular architecture with clear separation of concerns:

- **sqs-consumer**: Handles consumption of health messages from SQS
- **openai-client**: Manages OpenAI API integration for training plan generation
- **dynamodb**: Handles training metadata persistence
- **notifier**: Publishes notifications to SQS
- **application**: Main application module that orchestrates the entire process

## Features

- 🏃‍♂️ Consumes health messages from SQS queue
- 🤖 Generates personalized training plans using OpenAI GPT-4
- 💾 Stores training metadata in DynamoDB
- 📨 Publishes notifications to SQS queue
- 🐳 Docker support with multi-stage builds
- 📊 Monitoring with Prometheus and Grafana
- 🔍 Logging and error handling
- 🚀 CI/CD pipeline with GitHub Actions
- ☁️ AWS deployment support (ECS/EKS)

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.9+
- Docker and Docker Compose
- AWS CLI (for production deployment)
- OpenAI API key

### Local Development with Docker

1. Clone the repository:
```bash
git clone https://github.com/open-trainer/java-trainer.git
cd java-trainer
```

2. Copy environment configuration:
```bash
cp .env.example .env
```

3. Update `.env` with your OpenAI API key:
```bash
OPENAI_API_KEY=your-openai-api-key-here
```

4. Start the application with LocalStack:
```bash
docker-compose up -d
```

5. Check application health:
```bash
curl http://localhost:8080/actuator/health
```

### Local Development without Docker

1. Install dependencies:
```bash
mvn clean install
```

2. Start LocalStack separately:
```bash
docker run --rm -it -p 4566:4566 -p 4510-4559:4510-4559 localstack/localstack
```

3. Run the application:
```bash
export OPENAI_API_KEY=your-openai-api-key-here
export AWS_SQS_HEALTH_QUEUE_URL=http://localhost:4566/000000000000/health-queue
export AWS_SQS_NOTIFICATION_QUEUE_URL=http://localhost:4566/000000000000/notification-queue
cd application
mvn spring-boot:run
```

## Testing

### Run all tests:
```bash
mvn clean test
```

### Run tests for specific module:
```bash
cd sqs-consumer
mvn test
```

### Integration tests:
```bash
mvn clean verify
```

## Monitoring

The application includes comprehensive monitoring:

- **Health checks**: Available at `/actuator/health`
- **Metrics**: Available at `/actuator/metrics` and `/actuator/prometheus`
- **Grafana dashboards**: Available at `http://localhost:3000` (admin/admin)
- **Prometheus**: Available at `http://localhost:9090`

## API Usage

### Health Message Format

Send health messages to the SQS queue in the following format:

```json
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
  "preferences": {
    "workout_type": "strength_training",
    "duration": "45_minutes"
  },
  "current_fitness_level": "intermediate"
}
```

### Example Training Plan Response

The system generates comprehensive training plans with:

- Weekly workout schedules
- Exercise details with sets, reps, and instructions
- Nutrition guidelines
- Safety considerations
- Progressive difficulty levels

## Deployment

### AWS ECS Deployment

1. Configure AWS credentials:
```bash
aws configure
```

2. Create ECR repository:
```bash
aws ecr create-repository --repository-name java-trainer
```

3. Deploy using GitHub Actions or manually:
```bash
# Build and push image
docker build -t java-trainer .
aws ecr get-login-password | docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
docker tag java-trainer:latest <account-id>.dkr.ecr.us-east-1.amazonaws.com/java-trainer:latest
docker push <account-id>.dkr.ecr.us-east-1.amazonaws.com/java-trainer:latest

# Deploy to ECS
aws ecs update-service --cluster java-trainer-cluster --service java-trainer-service --force-new-deployment
```

### Kubernetes Deployment

```bash
# Apply Kubernetes manifests
kubectl apply -f k8s/
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key | Required |
| `AWS_REGION` | AWS region | `us-east-1` |
| `AWS_SQS_HEALTH_QUEUE_URL` | Health messages SQS queue URL | Required |
| `AWS_SQS_NOTIFICATION_QUEUE_URL` | Notifications SQS queue URL | Required |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `local` |
| `LOG_LEVEL` | Logging level | `INFO` |
| `APP_SCHEDULER_ENABLED` | Enable/disable message polling | `true` |
| `APP_SQS_POLL_INTERVAL` | SQS polling interval (ms) | `30000` |

### Spring Profiles

- **local**: For local development with LocalStack
- **staging**: For staging environment
- **production**: For production environment

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support and questions:
- Create an issue in the GitHub repository
- Check the documentation in the `/docs` folder
- Review the monitoring dashboards for operational insights
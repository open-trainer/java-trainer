# API Documentation

## Health Message Format

The application consumes health messages from an SQS queue. Messages should be in JSON format:

### Health Message Schema

```json
{
  "user_id": "string",
  "timestamp": "2024-01-15T10:30:00",
  "health_metrics": {
    "age": 30,
    "weight": 70.5,
    "height": 175.0,
    "heart_rate": 72,
    "blood_pressure": "120/80",
    "activity_level": "sedentary|light|moderate|active|very_active",
    "medical_conditions": ["condition1", "condition2"]
  },
  "goals": ["weight_loss", "muscle_gain", "endurance", "flexibility"],
  "preferences": {
    "workout_type": "strength_training|cardio|yoga|mixed",
    "duration": "30_minutes|45_minutes|60_minutes|90_minutes",
    "equipment": "none|basic|gym",
    "location": "home|gym|outdoor"
  },
  "current_fitness_level": "beginner|intermediate|advanced"
}
```

### Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `user_id` | string | Yes | Unique identifier for the user |
| `timestamp` | datetime | Yes | When the health data was recorded |
| `health_metrics.age` | integer | Yes | User's age in years |
| `health_metrics.weight` | number | Yes | User's weight in kg |
| `health_metrics.height` | number | Yes | User's height in cm |
| `health_metrics.heart_rate` | integer | No | Resting heart rate (bpm) |
| `health_metrics.blood_pressure` | string | No | Blood pressure reading (e.g., "120/80") |
| `health_metrics.activity_level` | string | Yes | Current activity level |
| `health_metrics.medical_conditions` | array | No | List of medical conditions |
| `goals` | array | Yes | User's fitness goals |
| `preferences` | object | No | User's workout preferences |
| `current_fitness_level` | string | Yes | Current fitness level |

### Example Health Message

```json
{
  "user_id": "user_12345",
  "timestamp": "2024-01-15T10:30:00",
  "health_metrics": {
    "age": 32,
    "weight": 75.5,
    "height": 180.0,
    "heart_rate": 68,
    "blood_pressure": "118/76",
    "activity_level": "moderate",
    "medical_conditions": ["none"]
  },
  "goals": ["weight_loss", "muscle_gain"],
  "preferences": {
    "workout_type": "mixed",
    "duration": "45_minutes",
    "equipment": "basic",
    "location": "home"
  },
  "current_fitness_level": "intermediate"
}
```

## Training Plan Response Format

The generated training plan follows this schema:

### Training Plan Schema

```json
{
  "plan_id": "string",
  "user_id": "string",
  "title": "string",
  "description": "string",
  "duration_weeks": 12,
  "difficulty_level": "beginner|intermediate|advanced",
  "weekly_schedule": [
    {
      "week": 1,
      "workouts": [
        {
          "day": "Monday",
          "type": "strength|cardio|flexibility|rest",
          "duration_minutes": 45,
          "exercises": [
            {
              "name": "Push-ups",
              "sets": 3,
              "reps": "8-12",
              "weight": "bodyweight",
              "instructions": "Keep body straight, lower to chest level"
            }
          ],
          "rest_periods": "60-90 seconds between sets"
        }
      ]
    }
  ],
  "nutrition_guidelines": "string",
  "safety_considerations": ["string"]
}
```

## Notification Message Format

Notifications are published to an SQS queue in this format:

### Notification Schema

```json
{
  "user_id": "string",
  "plan_id": "string",
  "notification_type": "TRAINING_PLAN_GENERATED|TRAINING_PLAN_ERROR",
  "title": "string",
  "message": "string",
  "timestamp": "2024-01-15T10:30:00",
  "priority": "LOW|MEDIUM|HIGH",
  "metadata": {
    "action_required": "VIEW_PLAN",
    "plan_title": "string"
  }
}
```

### Notification Types

| Type | Description | Priority |
|------|-------------|----------|
| `TRAINING_PLAN_GENERATED` | Training plan successfully created | HIGH |
| `TRAINING_PLAN_ERROR` | Error occurred during generation | MEDIUM |

## Actuator Endpoints

The application exposes Spring Boot Actuator endpoints:

### Health Check
```
GET /actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963170816,
        "free": 339931340800,
        "threshold": 10485760,
        "exists": true
      }
    }
  }
}
```

### Metrics
```
GET /actuator/metrics
```

Lists all available metrics.

### Prometheus Metrics
```
GET /actuator/prometheus
```

Returns metrics in Prometheus format for scraping.

### Application Info
```
GET /actuator/info
```

Returns application information and build details.

## Error Handling

The application implements comprehensive error handling:

### Error Response Format

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Training plan generation failed",
  "path": "/api/training-plan"
}
```

### Common Error Scenarios

1. **Invalid Health Message**: Malformed JSON or missing required fields
2. **OpenAI API Errors**: Rate limiting, authentication, or service unavailability
3. **AWS Service Errors**: SQS, DynamoDB connection issues
4. **Processing Timeouts**: Long-running operations exceeding timeout limits

## Rate Limiting and Quotas

- **OpenAI API**: Respects OpenAI rate limits with retry logic
- **AWS Services**: Uses AWS SDK built-in retry mechanisms
- **Message Processing**: Configurable polling intervals to manage load

## Security Considerations

- All sensitive data (API keys, credentials) stored in environment variables or AWS Secrets Manager
- No sensitive information logged
- AWS IAM roles for service-to-service authentication
- Health endpoints don't expose sensitive information
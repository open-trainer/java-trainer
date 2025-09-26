# Multi-stage Docker build
FROM gradle:8.5-jdk17 AS builder

# Set working directory
WORKDIR /app

# Copy Gradle files for dependency caching
COPY build.gradle settings.gradle gradlew ./
COPY gradle gradle
COPY sqs-consumer/build.gradle sqs-consumer/
COPY openai-client/build.gradle openai-client/
COPY dynamodb/build.gradle dynamodb/
COPY notifier/build.gradle notifier/
COPY application/build.gradle application/

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY sqs-consumer/src sqs-consumer/src
COPY openai-client/src openai-client/src
COPY dynamodb/src dynamodb/src
COPY notifier/src notifier/src
COPY application/src application/src

# Build application
RUN ./gradlew clean bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Set working directory
WORKDIR /app

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install required packages
RUN apk add --no-cache curl

# Copy application JAR from Gradle build
COPY --from=builder /app/application/build/libs/application-1.0.0.jar app.jar

# Change ownership
RUN chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Set JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
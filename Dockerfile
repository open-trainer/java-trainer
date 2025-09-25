# Multi-stage Docker build
FROM maven:3.9.5-openjdk-17 AS builder

# Set working directory
WORKDIR /app

# Copy pom files for dependency resolution
COPY pom.xml .
COPY sqs-consumer/pom.xml sqs-consumer/
COPY openai-client/pom.xml openai-client/
COPY dynamodb/pom.xml dynamodb/
COPY notifier/pom.xml notifier/
COPY application/pom.xml application/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY sqs-consumer/src sqs-consumer/src
COPY openai-client/src openai-client/src
COPY dynamodb/src dynamodb/src
COPY notifier/src notifier/src
COPY application/src application/src

# Build application
RUN mvn clean package -DskipTests

# Runtime stage
FROM openjdk:17-jre-slim

# Set working directory
WORKDIR /app

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy application JAR
COPY --from=builder /app/application/target/application-1.0.0.jar app.jar

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
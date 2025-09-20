# Multi-stage build for smaller final image
FROM maven:3.9-eclipse-temurin-17 AS builder

LABEL maintainer="FreeSideNomad"
LABEL description="Proxima - JWT Header Injection Reverse Proxy"

# Create app directory
WORKDIR /app

# Copy maven files first for better layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN mvn clean package -DskipTests

# Final runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy only the built JAR from the builder stage
COPY --from=builder /app/target/proxima-*.jar app.jar

# Create volume for config
VOLUME ["/app/config"]

# Expose ports
EXPOSE 8080

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=docker

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.config.additional-location=file:/app/config/"]
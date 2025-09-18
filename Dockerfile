FROM openjdk:17-jdk-slim

LABEL maintainer="FreeSideNomad"
LABEL description="Proxima - JWT Header Injection Reverse Proxy"

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Create app directory
WORKDIR /app

# Copy maven files
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src src

# Build application
RUN mvn clean package -DskipTests

# Create volume for config
VOLUME ["/app/config"]

# Expose ports
EXPOSE 8080

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV SPRING_PROFILES_ACTIVE=docker

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar target/proxima-*.jar --spring.config.additional-location=file:/app/config/"]
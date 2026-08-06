# ==========================================
# STAGE 1: Build Kotlin Spring Boot Application
# ==========================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy Gradle wrapper and configuration files
COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./

# Make gradlew executable
RUN chmod +x gradlew

# Copy source code
COPY src src

# Build executable Fat JAR file
RUN ./gradlew bootJar --no-daemon -x test

# ==========================================
# STAGE 2: Lightweight JRE Runtime Environment
# ==========================================
FROM eclipse-temurin:17-jre-alpine AS runner

WORKDIR /app

# Create non-root system user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled JAR artifact from builder stage
COPY --from=builder /build/build/libs/*.jar /app/app.jar

# Set file ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose Spring Boot HTTP Port
EXPOSE 8080

# Environment variables
ENV PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Launch Spring Boot Application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

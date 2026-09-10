# ============================================================
# ShopStack Backend — Dockerfile
# Multi-stage build: compile with Maven, run with slim JRE
# ============================================================

# --- Stage 1: Build ---
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and POM first (cache dependency layer)
COPY pom.xml .
COPY src ./src

# Build the JAR (skip tests — tests run in CI, not Docker build)
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests -q

# --- Stage 2: Run ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S shopstack && adduser -S shopstack -G shopstack

# Copy compiled JAR from builder stage
COPY --from=builder /app/target/shopstack-backend-*.jar app.jar

# Set ownership
RUN chown shopstack:shopstack app.jar

USER shopstack

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

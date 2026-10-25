# ==============================================================
# Finance System Manager - Production Multi-Stage Dockerfile
# Compatible with Render, Railway, Fly.io, AWS, and GCP Cloud Run
# ==============================================================

# Stage 1: Build JAR using Eclipse Temurin JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Copy Maven wrapper and backend source
COPY backend/pom.xml ./pom.xml
COPY backend/mvnw ./mvnw
COPY backend/.mvn ./.mvn
COPY backend/src ./src

# Ensure Unix line endings for maven wrapper and build jar
RUN sed -i 's/\r$//' ./mvnw && chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal JRE 21 Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Ensure data directory exists for embedded database storage
RUN mkdir -p /app/data

# Copy built artifact
COPY --from=builder /build/target/finance-system-manager-1.0.0.jar /app/app.jar

# Dynamic Port Binding (Render/Railway/Heroku/CloudRun set $PORT)
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]

FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy backend pom
COPY backend/pom.xml ./backend/

# Download dependencies
RUN cd backend && mvn dependency:resolve

# Copy backend source
COPY backend/src ./backend/src

# Build backend
RUN cd backend && mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy built JAR from builder
COPY --from=builder /build/backend/target/quiz-system-1.0.0.jar ./app.jar

# Copy frontend static files (optional)
COPY frontend ./frontend

# Expose port
EXPOSE 8080

# Set environment
ENV SPRING_PROFILES_ACTIVE=railway
ENV PORT=8080

# Start application
ENTRYPOINT ["java", "-Dserver.port=8080", "-jar", "app.jar"]

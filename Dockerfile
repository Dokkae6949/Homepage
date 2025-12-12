# --- Stage 1: Build the JAR ---
FROM gradle:9.2.1-jdk21 AS build

# Set working dir
WORKDIR /app

# Copy Gradle files first (for caching)
COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts gradle.properties gradlew ./
COPY --chown=gradle:gradle gradle ./gradle

# Copy source code
COPY --chown=gradle:gradle src ./src

# Build the fat jar
RUN ./gradlew clean shadowJar --no-daemon

# --- Stage 2: Run the app ---
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port (same as your server)
EXPOSE 9000

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]

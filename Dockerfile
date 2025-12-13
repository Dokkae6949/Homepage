# --- Stage 1: Build the JAR ---
FROM gradle:9.2.1-jdk21 AS build

# Set working dir
WORKDIR /app

# Copy Gradle files first (for caching)
COPY --chown=gradle:gradle build.gradle.kts settings.gradle.kts gradle.properties gradlew ./
COPY --chown=gradle:gradle gradle ./gradle

# Copy source code
COPY --chown=gradle:gradle src ./src

# Copy pre-generated code fragments
COPY --chown=gradle:gradle build/generated-src ./build/generated-src
COPY --chown=gradle:gradle build/generated-resources ./build/generated-resources

# Build the fat jar without cleaning (preserves generated code)
RUN ./gradlew build -x clean -x cleanGenerated -x jooqCodegen -x flywayMigrate -x precompileJte --no-daemon

# --- Stage 2: Run the app ---
FROM eclipse-temurin:21-jdk-alpine

ARG PORT=9000

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port (same as your server)
EXPOSE ${PORT}

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]

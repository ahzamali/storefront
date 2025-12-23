FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy the JAR file built by Maven
COPY server/target/*.jar app.jar

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

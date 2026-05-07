FROM openjdk:17-slim

# Set the working directory in the container
WORKDIR /app

# Copy the JAR file from your build output to the container
COPY build/libs/coin-account-app-0.0.1-SNAPSHOT.jar app.jar

# Make port 8090 available to the world outside this container
EXPOSE 8090

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]

# Add health check
HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:8090/actuator/health || exit 1

# Add labels for better maintainability
LABEL maintainer="Stanislav Ivan <stanislav.ivan.1@gmail.com>"
LABEL version="0.0.1"
LABEL description="Coin Account Application Docker Image"
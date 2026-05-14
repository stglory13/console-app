FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY build/libs/flyapp-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://localhost:8080/flyapp/actuator/health || exit 1

LABEL maintainer="Stanislav Ivan <stanislav.ivan.1@gmail.com>"
LABEL version="0.0.1"
LABEL description="Fly Application Docker Image"
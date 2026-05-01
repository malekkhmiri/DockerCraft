FROM eclipse-temurin:17-jdk-alpine
RUN apk add --no-cache docker-cli
WORKDIR /app
COPY target/*-SNAPSHOT.jar app.jar
EXPOSE  9016
ENTRYPOINT ["java", "-jar", "app.jar"]

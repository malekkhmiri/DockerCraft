FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*-SNAPSHOT.jar app.jar
EXPOSE 9012
ENTRYPOINT ["java", "-jar", "app.jar"]

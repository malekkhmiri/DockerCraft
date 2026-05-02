FROM eclipse-temurin:17-jdk-alpine
RUN apk add --no-cache docker-cli
WORKDIR /app
COPY target/dockerfile-service-*.jar app.jar
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]

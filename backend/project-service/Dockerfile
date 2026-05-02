FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/project-service-*.jar app.jar
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]

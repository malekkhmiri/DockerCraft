FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-eng \
    tesseract-ocr-fra \
    tesseract-ocr-ara \
    libgomp1 \
    libgl1-mesa-glx \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY target/*-SNAPSHOT.jar app.jar

EXPOSE 9014
ENTRYPOINT ["java", "-jar", "app.jar"]

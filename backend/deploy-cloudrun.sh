#!/bin/bash

# Configuration
PROJECT_ID=$(gcloud config get-value project)
REGION="us-central1"

echo "🚀 Déploiement de DockerCraft via LOCAL BUILD (Cloud Shell)..."

# 1. Déploiement de Ollama (IA)
echo "🤖 Déploiement de dc-ollama..."
gcloud run deploy dc-ollama \
    --image docker.io/ollama/ollama:latest \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --memory 4Gi \
    --cpu 2 \
    --port 11434

OLLAMA_URL=$(gcloud run services describe dc-ollama --region $REGION --format='value(status.url)')
echo "✅ Ollama prêt à : $OLLAMA_URL"

# 2. Compilation du JAR (On utilise le Maven du Cloud Shell)
echo "📦 Compilation du JAR avec Maven..."
mvn clean package -pl dockerfile-service -am -DskipTests

# 3. Construction locale de l'image Docker (Dans le Cloud Shell)
echo "🐳 Construction locale de l'image Docker pour Docker Hub..."
DOCKER_USER="malekkhmiri"
docker build -t $DOCKER_USER/dockergeneration-dockerfile-service:latest ./dockerfile-service

# 4. Poussée de l'image vers Docker Hub
echo "📤 Poussée de l'image vers Docker Hub ($DOCKER_USER)..."
docker push $DOCKER_USER/dockergeneration-dockerfile-service:latest

# 5. Déploiement sur Cloud Run
echo "📦 Déploiement final sur dc-dockerfile-service (via Docker Hub)..."
gcloud run deploy dc-dockerfile-service \
    --image docker.io/$DOCKER_USER/dockergeneration-dockerfile-service:latest \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --memory 4Gi \
    --cpu 2 \
    --timeout 300 \
    --set-env-vars="SPRING_PROFILES_ACTIVE=prod,SERVER_PORT=8080,OLLAMA_URL=$OLLAMA_URL,SPRING_DATASOURCE_URL=jdbc:h2:mem:dockercraft;DB_CLOSE_DELAY=-1,SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver,SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect,EUREKA_CLIENT_ENABLED=false,SPRING_CLOUD_CONFIG_ENABLED=false,SPRING_CONFIG_IMPORT_CHECK_ENABLED=false,SPRING_RABBITMQ_ENABLED=false"

SERVICE_URL=$(gcloud run services describe dc-dockerfile-service --region $REGION --format='value(status.url)')

echo "--------------------------------------------------"
echo "🎉 DEPLOIEMENT REUSSI VIA CLOUD SHELL !"
echo "🔗 URL DU SERVICE : $SERVICE_URL"
echo "--------------------------------------------------"

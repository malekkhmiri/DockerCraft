#!/bin/bash
set -e


# Configuration
PROJECT_ID=$(gcloud config get-value project)
REGION="us-central1"
DOCKER_USER="malekkhmiri"

echo "🚀 Déploiement de User Service via LOCAL BUILD (Cloud Shell)..."

# 1. Compilation du JAR
echo "📦 Compilation du JAR avec Maven..."
mvn clean package -pl user-service -am -DskipTests

# 2. Construction locale de l'image Docker
echo "🐳 Construction locale de l'image Docker pour Docker Hub..."
docker build -t docker.io/$DOCKER_USER/dockergeneration-user-api:latest ./user-service

# 3. Poussée de l'image vers Docker Hub
echo "📤 Poussée de l'image vers Docker Hub..."
docker push docker.io/$DOCKER_USER/dockergeneration-user-api:latest

# 4. Déploiement sur Cloud Run
echo "📦 Déploiement final sur dc-user-service (via Docker Hub)..."
gcloud run deploy dc-user-service \
    --image docker.io/$DOCKER_USER/dockergeneration-user-api:latest \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --memory 2Gi \
    --cpu 1 \
    --timeout 300 \
    --set-env-vars="SPRING_PROFILES_ACTIVE=prod,SPRING_DATASOURCE_URL=jdbc:h2:mem:userdb;DB_CLOSE_DELAY=-1,SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver,SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect,EUREKA_CLIENT_ENABLED=false,SPRING_CLOUD_DISCOVERY_ENABLED=false,SPRING_CLOUD_CONFIG_ENABLED=false,SPRING_CONFIG_IMPORT_CHECK_ENABLED=false,SPRING_RABBITMQ_ENABLED=false,LOGGING_LEVEL_ROOT=INFO"

SERVICE_URL=$(gcloud run services describe dc-user-service --region $REGION --format='value(status.url)')

echo "--------------------------------------------------"
echo "🚀 TENTATIVE DE RECUPERATION DES LOGS (Diagnostic)..."
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=dc-user-service" --limit 20 --format="value(textPayload)"
echo "--------------------------------------------------"

echo "🎉 DEPLOIEMENT TERMINE !"
echo "🔗 URL DU SERVICE : $SERVICE_URL"
echo "--------------------------------------------------"

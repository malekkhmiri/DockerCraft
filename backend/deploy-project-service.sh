#!/bin/bash
set -e

# Configuration
PROJECT_ID=$(gcloud config get-value project)
REGION="us-central1"
DOCKER_USER="malekkhmiri"

echo "🚀 Déploiement de Project Service via LOCAL BUILD (Cloud Shell)..."

# 1. Compilation du JAR
echo "📦 Compilation du JAR avec Maven..."
mvn clean package -pl project-service -am -DskipTests

# 1. Construction locale de l'image Docker avec un TAG UNIQUE
TAG=$(date +%s)
IMAGE_NAME="docker.io/$DOCKER_USER/dockergeneration-project-api:$TAG"

echo "🐳 Construction locale de l'image Docker : $IMAGE_NAME..."
docker build --provenance=false -t $IMAGE_NAME ./project-service

# 2. Poussée de l'image vers Docker Hub
echo "📤 Poussée de l'image vers Docker Hub..."
docker push $IMAGE_NAME

# 3. Déploiement sur Cloud Run
echo "📦 Déploiement final sur dc-project-service (Tag: $TAG)..."
gcloud run deploy dc-project-service \
    --image $IMAGE_NAME \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --memory 2Gi \
    --cpu 1 \
    --timeout 300 \
    --set-env-vars="SPRING_PROFILES_ACTIVE=prod,DOCKERFILE_SERVICE_URL=https://dc-dockerfile-service-715286351060.us-central1.run.app,SPRING_DATASOURCE_URL=jdbc:h2:mem:projectdb;DB_CLOSE_DELAY=-1,SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver,SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect,EUREKA_CLIENT_ENABLED=false,SPRING_CLOUD_DISCOVERY_ENABLED=false,SPRING_CLOUD_CONFIG_ENABLED=false,SPRING_CONFIG_IMPORT_CHECK_ENABLED=false,SPRING_RABBITMQ_ENABLED=false,LOGGING_LEVEL_ROOT=INFO"

SERVICE_URL=$(gcloud run services describe dc-project-service --region $REGION --format='value(status.url)')

echo "--------------------------------------------------"
echo "🚀 TENTATIVE DE RECUPERATION DES LOGS (Diagnostic)..."
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=dc-project-service" --limit 20 --format="value(textPayload)"
echo "--------------------------------------------------"

echo "🎉 DEPLOIEMENT TERMINE !"
echo "🔗 URL DU SERVICE : $SERVICE_URL"
echo "--------------------------------------------------"

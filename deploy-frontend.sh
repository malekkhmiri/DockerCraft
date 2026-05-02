#!/bin/bash
set -e

# Configuration
PROJECT_ID=$(gcloud config get-value project)
REGION="us-central1"
DOCKER_USER="malekkhmiri"

echo "🚀 Déploiement du Frontend via LOCAL BUILD (Cloud Shell)..."

# 1. Construction locale de l'image Docker avec un TAG UNIQUE
TAG=$(date +%s)
IMAGE_NAME="docker.io/$DOCKER_USER/dockergeneration-frontend:$TAG"

echo "🐳 Construction de l'image $IMAGE_NAME..."
docker build --no-cache -t $IMAGE_NAME ./frontend

# 2. Poussée de l'image vers Docker Hub
echo "📤 Poussée de l'image vers Docker Hub..."
docker push $IMAGE_NAME

# 3. Déploiement sur Cloud Run avec l'image spécifique
echo "📦 Déploiement final sur dc-frontend (Tag: $TAG)..."
gcloud run deploy dc-frontend \
    --image $IMAGE_NAME \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --memory 1Gi \
    --cpu 1 \
    --timeout 300

SERVICE_URL=$(gcloud run services describe dc-frontend --region $REGION --format='value(status.url)')

echo "--------------------------------------------------"
echo "🎉 DEPLOIEMENT TERMINE !"
echo "🔗 URL DE LA PLATEFORME : $SERVICE_URL"
echo "--------------------------------------------------"

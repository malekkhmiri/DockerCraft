#!/bin/bash
set -e

# Configuration
PROJECT_ID=$(gcloud config get-value project)
REGION="us-central1"
DOCKER_USER="malekkhmiri"

echo "🚀 Déploiement du Frontend via LOCAL BUILD (Cloud Shell)..."

# 1. Construction locale de l'image Docker
echo "🐳 Construction locale de l'image Docker pour Docker Hub..."
docker build -t docker.io/$DOCKER_USER/dockergeneration-frontend:latest ./frontend

# 2. Poussée de l'image vers Docker Hub
echo "📤 Poussée de l'image vers Docker Hub..."
docker push docker.io/$DOCKER_USER/dockergeneration-frontend:latest

# 3. Déploiement sur Cloud Run
echo "📦 Déploiement final sur dc-frontend (via Docker Hub)..."
gcloud run deploy dc-frontend \
    --image docker.io/$DOCKER_USER/dockergeneration-frontend:latest \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --memory 1Gi \
    --cpu 1 \
    --timeout 300 \
    --set-env-vars="PORT=8080"

SERVICE_URL=$(gcloud run services describe dc-frontend --region $REGION --format='value(status.url)')

echo "--------------------------------------------------"
echo "🎉 DEPLOIEMENT TERMINE !"
echo "🔗 URL DE LA PLATEFORME : $SERVICE_URL"
echo "--------------------------------------------------"

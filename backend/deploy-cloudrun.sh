#!/bin/bash

# Configuration
PROJECT_ID=$(gcloud config get-value project)
REGION="us-central1"

echo "🚀 Déploiement de DockerCraft sur Cloud Run (Mode Serverless)..."

# 1. Déploiement du Dockerfile Service (celui que nous avons durci)
echo "📦 Déploiement de dc-dockerfile-service..."
gcloud run deploy dc-dockerfile-service \
    --image malekkhmiri/dockergeneration-dockerfile-service:latest \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --set-env-vars="SPRING_PROFILES_ACTIVE=prod,SERVER_PORT=8080"

# Note: Cloud Run utilise par défaut le port 8080, on s'adapte.

echo "✅ Dockerfile Service déployé !"
echo "🔗 URL : $(gcloud run services describe dc-dockerfile-service --region $REGION --format='value(status.url)')"

# On pourra ajouter les autres services ici (User, Project, Gateway)

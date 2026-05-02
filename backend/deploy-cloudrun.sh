#!/bin/bash

# Configuration
PROJECT_ID=$(gcloud config get-value project)
REGION="us-central1"

echo "🚀 Déploiement du Dockerfile Service sur Cloud Run..."

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

# 2. Déploiement du Dockerfile Service (Le cœur de notre travail)
echo "📦 Déploiement de dc-dockerfile-service..."
gcloud run deploy dc-dockerfile-service \
    --image docker.io/malekkhmiri/dockergeneration-dockerfile-service:latest \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --set-env-vars="SPRING_PROFILES_ACTIVE=prod,SERVER_PORT=8080,OLLAMA_URL=$OLLAMA_URL,SPRING_DATASOURCE_URL=jdbc:h2:mem:dockercraft;DB_CLOSE_DELAY=-1,SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver,SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect,EUREKA_CLIENT_ENABLED=false,SPRING_CLOUD_CONFIG_ENABLED=false,SPRING_RABBITMQ_ENABLED=false"

SERVICE_URL=$(gcloud run services describe dc-dockerfile-service --region $REGION --format='value(status.url)')

echo "--------------------------------------------------"
echo "🎉 DEPLOIEMENT REUSSI !"
echo "🔗 URL DIRECTE DU SERVICE : $SERVICE_URL"
echo "--------------------------------------------------"
echo "Note : La Gateway a été retirée pour simplifier le déploiement en environnement de workshop."

#!/bin/bash

# Configuration
PROJECT_ID=$(gcloud config get-value project)
REGION="us-central1"

echo "🚀 Déploiement TOTAL de DockerCraft sur Cloud Run (Mode Workshop)..."

# 1. Déploiement de Ollama (IA)
echo "🤖 Déploiement de dc-ollama..."
gcloud run deploy dc-ollama \
    --image ollama/ollama:latest \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --memory 4Gi \
    --cpu 2 \
    --port 11434

OLLAMA_URL=$(gcloud run services describe dc-ollama --region $REGION --format='value(status.url)')
echo "✅ Ollama prêt à : $OLLAMA_URL"

# 2. Déploiement du Dockerfile Service
echo "📦 Déploiement de dc-dockerfile-service..."
gcloud run deploy dc-dockerfile-service \
    --image malekkhmiri/dockergeneration-dockerfile-service:latest \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --set-env-vars="SPRING_PROFILES_ACTIVE=prod,SERVER_PORT=8080,OLLAMA_URL=$OLLAMA_URL,SPRING_DATASOURCE_URL=jdbc:h2:mem:dockercraft;DB_CLOSE_DELAY=-1,SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver,SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect,EUREKA_CLIENT_ENABLED=false,SPRING_CLOUD_CONFIG_ENABLED=false,SPRING_RABBITMQ_ENABLED=false"

echo "✅ Dockerfile Service déployé !"

# 3. Déploiement de la Gateway (Facultatif en Cloud Run mais utile pour l'URL unique)
echo "🌐 Déploiement de dc-gateway..."
gcloud run deploy dc-gateway \
    --image malekkhmiri/dockergeneration-gateway:latest \
    --platform managed \
    --region $REGION \
    --allow-unauthenticated \
    --set-env-vars="SERVER_PORT=8080,SPRING_DATASOURCE_URL=jdbc:h2:mem:gateway;DB_CLOSE_DELAY=-1,EUREKA_CLIENT_ENABLED=false,SPRING_CLOUD_CONFIG_ENABLED=false"

GATEWAY_URL=$(gcloud run services describe dc-gateway --region $REGION --format='value(status.url)')

echo "--------------------------------------------------"
echo "🎉 DEPLOIEMENT TERMINE AVEC SUCCES !"
echo "🔗 Accès à la Gateway : $GATEWAY_URL"
echo "🔗 Accès direct au Dockerfile Service : $(gcloud run services describe dc-dockerfile-service --region $REGION --format='value(status.url)')"
echo "--------------------------------------------------"

#!/bin/bash

# Configuration
PROJECT_ID=$(gcloud config get-value project)
CLUSTER_NAME="dockercraft-cluster"
REGION="us-central1"
NAMESPACE="dockercraft"

echo "🚀 Starting Deployment of DockerCraft to GKE..."

# 1. Create the Cluster (if not exists)
echo "📦 Creating GKE Cluster..."
gcloud container clusters create $CLUSTER_NAME \
    --region $REGION \
    --num-nodes 3 \
    --machine-type e2-medium

# 2. Get Credentials
gcloud container clusters get-credentials $CLUSTER_NAME --region $REGION

# 3. Create Static IP for Ingress
echo "🌐 Allocating Static IP..."
gcloud compute addresses create dockercraft-ip --global

# 4. Apply Kubernetes Manifests
echo "⚙️ Applying Manifests..."

kubectl apply -f k8s/config/common.yaml
kubectl apply -f k8s/infrastructure/core.yaml

echo "⏳ Waiting for Infrastructure to be ready..."
kubectl wait --for=condition=ready pod -l app=dc-db -n $NAMESPACE --timeout=120s
kubectl wait --for=condition=ready pod -l app=dc-rabbitmq -n $NAMESPACE --timeout=120s

# Deploy Services (Apply all yaml in services dir)
kubectl apply -f k8s/services/

# Deploy Network (Ingress & Ollama)
kubectl apply -f k8s/network/ingress.yaml

echo "✅ Deployment finished!"
echo "📍 Your app will be available at the IP address associated with 'dockercraft-ip' once the Ingress is ready."
echo "🔗 Check progress: kubectl get ingress -n $NAMESPACE"

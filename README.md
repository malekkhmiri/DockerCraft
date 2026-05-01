# DockerGeneration 🚀

**DockerGeneration** is an AI-powered platform for automated Dockerfile generation and CI/CD orchestration. It analyzes your project structure and generates optimal, production-ready Dockerfiles using CodeLlama.

## 🏗️ Architecture
- **Backend**: Microservices built with Spring Boot 3.4
  - `Gateway`: API entry point with security.
  - `Discovery`: Service discovery (Eureka).
  - `Config Server`: Centralized configuration.
  - `User Service`: RBAC and Auth.
  - `Project Service`: ZIP upload and extraction.
  - `Dockerfile Service`: AI generation (Ollama/CodeLlama).
  - `Pipeline Service`: CI/CD trigger and monitoring.
  - `Image Service`: Docker registry metadata.
- **Frontend**: Modern Angular 19 with a premium dark theme.
- **Infrastructure**: RabbitMQ for event-driven flows, PostgreSQL for persistence.

## 🚀 Getting Started

### Prerequisites
- Docker & Docker Compose
- [Ollama](https://ollama.ai/) installed locally
- `ollama pull codellama:7b`

### Run with Docker Compose
1. Clone the repository into `AiDoDev`.
2. Configure `.env` file with your credentials (DB, RabbitMQ, etc.).
3. Run the orchestration:
   ```bash
   docker-compose up --build
   ```

### Frontend Development
```bash
cd frontend
npm install
ng serve
```

## 🤖 AI Integration
DockerGeneration uses **Ollama** with the `codellama` model. Ensure Ollama is running and accessible at `http://localhost:11434`.

## 🛡️ Role-Based Access Control
- **Admin**: Full platform supervision, user management, and global monitoring.
- **Normal User (Pro/Student)**: Project upload, AI generation, and CI/CD pipelines.

## ☁️ Cloud Deployment
Ready for Kubernetes. Manifests are located in `/infra/k8s-manifests.yaml`.
CI/CD is handled via GitHub Actions in `.github/workflows/deploy.yml`.

---
*Built with ❤️ for Aidodev.*

# DockerGen AI / DockerGeneration ðŸš€

Plateforme moderne de gÃ©nÃ©ration de Dockerfiles et de gestion de pipelines CI/CD alimentÃ©e par l'Intelligence Artificielle (Ollama).

## ðŸ—ï¸ Architecture du SystÃ¨me

```mermaid
graph TD
    Client[Angular Frontend] -->|HTTP| Gateway[API Gateway :8080]
    
    subgraph "Core Microservices"
        Gateway --> UserSvc[User Service :8081]
        Gateway --> ProjSvc[Project Service :8082]
        Gateway --> DockerSvc[Dockerfile Service :8083]
        Gateway --> PipeSvc[Pipeline Service :8084]
        Gateway --> ImgSvc[Image Service :8085]
    end

    subgraph "Infrastructure"
        UserSvc --> DB[(PostgreSQL)]
        ProjSvc --> DB
        DockerSvc --> DB
        PipeSvc --> DB
        ImgSvc --> DB
        
        AllSvc[Microservices] -->|Service Discovery| Eureka[Eureka Discovery :8761]
        AllSvc -->|Config| ConfigSvc[Config Server :8888]
        AllSvc -->|Messaging| RabbitMQ[RabbitMQ :5672]
        
        UserSvc --> AI[Ollama AI :11434]
        DockerSvc --> AI
    end
    
    subgraph "Tools"
        Sonar[SonarQube :9000]
    end
```

## ðŸ› ï¸ Microservices & Technologies

| Service | Description | Port |
| :--- | :--- | :--- |
| **Config Server** | Centralisation des fichiers de configuration (Git repo). | 8888 |
| **Discovery** | Service Discovery via Netflix Eureka. | 8761 |
| **Gateway** | Point d'entrÃ©e unique, routage et sÃ©curitÃ©. | 8080 |
| **User Service** | Authentification, gestion des rÃ´les et vÃ©rification Ã©tudiant (IA). | 8081 |
| **Project Service** | Gestion des projets de dÃ©ploiement des utilisateurs. | 8082 |
| **Dockerfile Service** | GÃ©nÃ©ration de Dockerfiles optimisÃ©s via LLM. | 8083 |
| **Pipeline Service** | Orchestration des builds et dÃ©ploiements. | 8084 |
| **Image Service** | Gestion et stockage des mÃ©tadonnÃ©es d'images Docker. | 8085 |

### Stack Technique
- **Backend :** Java 17, Spring Boot 3.4, Spring Cloud, Spring Data JPA.
- **Frontend :** Angular 19, RxJS, WebSocket (STOMP).
- **Base de donnÃ©es :** PostgreSQL.
- **Messaging :** RabbitMQ (Events distributed).
- **IA :** Ollama (Llama/Mistral models).
- **QualitÃ© :** SonarQube.

## ðŸš€ Lancement Rapide

### PrÃ©requis
- Docker & Docker Compose
- Java 17+ (pour le dÃ©veloppement local)
- Node.js & Angular CLI (pour le frontend)

### Installation
1.  **Cloner le projet**
2.  **Lancer l'infrastructure complÃ¨te :**
    ```bash
    cd microservice
    docker-compose up -d
    ```
3.  **Lancer le Frontend :**
    ```bash
    cd frontend-platform
    npm install
    npm start
    ```

## ðŸ”’ SÃ©curitÃ©
- Authentification basÃ©e sur les **JWT (JSON Web Tokens)**.
- Chaque service valide le token via un filtre de sÃ©curitÃ© partagÃ© (en cours de centralisation).
- RÃ´les supportÃ©s : `STUDENT`, `PROFESSIONAL`, `ADMIN`.

## ðŸ“ˆ Monitoring (Ã€ venir)
- [ ] Integration de Prometheus & Grafana.
- [ ] Tracing distribuÃ© avec Micrometer & Zipkin.

---
*Maintenu par malekkhmiri-group.*


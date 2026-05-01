A cloud-native microservices ecosystem designed for scalability, observability, and security.

--------------------------------------------------------------------------------------------------
pre-requities : 
1: java 21+
2: docker
3: kubernetes
4: npm
5 : google oauth credentials
---------------------------------------------------------------------------------------------------

Architecture Highlights
Framework: Spring Boot 4.0.5, Java 21.

Service Mesh: Istio with strict mTLS enabled (PeerAuthentication).

Data Persistence: PostgreSQL with JPA, Redis for caching.

Messaging: Apache Kafka (3.8.0) for asynchronous event-driven communication.

Observability:

Metrics: Prometheus + Grafana (with Actuator integration).

Logs: Fluent Bit -> Kafka -> Logstash -> Elasticsearch.

Containerization: Multi-stage Docker builds using layered JARs for optimized deployment.

Prerequisites
JDK 21

Docker (with Kubernetes enabled, e.g., Docker Desktop or Minikube)

kubectl

Istio (installed in your cluster)

Setup and Deployment
1. Initialize Infrastructure
Create the dedicated namespace and apply the infrastructure manifests:

bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/PeerAuthentication.yaml
kubectl apply -f k8s/postgres-server-deployment.yaml
kubectl apply -f k8s/redis-deployment.yaml
kubectl apply -f k8s/kafka-dev-cluster.yaml
2. Deploy Observability Stack
bash
kubectl apply -f k8s/prometheus-setup.yaml
kubectl apply -f k8s/logstash-deployment.yaml
# Deploy Grafana and Elasticsearch configurations
kubectl apply -f k8s/grafana-deployment.yaml
3. Build and Deploy Application
From the Response_Server directory:

bash
# Build the JAR
mvn clean package

# Build the Docker image
docker build -t sidbhatt94/response-server .

# Push/Load image to your K8s node
# Deploy the service
kubectl apply -f k8s/response-service-deployment.yaml
Running Locally for Development
To run the Response_Server locally (outside of Kubernetes) for rapid testing, follow these steps:

Prerequisites for Local Run
Start Dependencies: Ensure you have local instances of PostgreSQL (on port 5432), Redis (6379), and Kafka (9092) running. You can use Docker Compose for this:

text
# Minimal docker-compose.yml for local testing
services:
  postgres:
    image: postgres:17-alpine
    ports: ["5432:5432"]
    environment: [POSTGRES_USER=postgres, POSTGRES_PASSWORD=root, POSTGRES_DB=postgresjpa]
  redis:
    image: redis
    ports: ["6379:6379"]
  kafka:
    image: apache/kafka:3.8.0
    ports: ["9092:9092"]
Run Command
Set the environment variables or rely on the application.properties defaults, then execute:

bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DDB_HOST=localhost -DREDIS_HOST=localhost -DKAFKA_HOST=localhost"
Accessing the Application
API Port: http://localhost:8154

Actuator Health: http://localhost:8154/actuator/health

Metrics: http://localhost:8154/actuator/prometheus


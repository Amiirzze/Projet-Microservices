# Gestion de Stock — Microservices Kubernetes + gRPC

## Architecture

```
[Navigateur / curl]
        │  HTTP  (stock.info)
        ▼
┌─────────────────────────────────┐
│  Nginx Ingress Controller       │
│  host: stock.info → :8080       │
└──────────────┬──────────────────┘
               │  HTTP REST
               ▼
      stock-client Pod  (Service 1)
      Spring Boot :8080
               │
               │  gRPC :9090
               │  • GetProduct     (appel unaire)
               │  • ManageStock    (streaming bidirectionnel)
               ▼
      stock-server Pod  (Service 2)
      Spring Boot :9090/:8081
               │  JDBC/JPA :5432
               ▼
      postgres Pod  →  PersistentVolume 5Gi

[ RBAC: stock-service-account — lecture seule ]
```

## Stack technique

| Composant      | Technologie                          |
|----------------|--------------------------------------|
| Service 1      | Java 17 / Spring Boot 3.3 / REST API |
| Service 2      | Java 17 / Spring Boot 3.3 / gRPC     |
| Communication  | gRPC / Protocol Buffers v3           |
| Base de données| PostgreSQL 15 + Spring Data JPA      |
| Orchestration  | Kubernetes (Minikube)                |
| Build          | Docker multi-stage (gradle:8.5-jdk17)|
| Ingress        | Nginx Ingress Controller             |
| Sécurité       | RBAC Kubernetes + Secret Opaque      |

---

## Structure du projet

```
stock-final/
├── Dockerfile.server          # Build multi-stage stock-server (sans gradlew local)
├── Dockerfile.client          # Build multi-stage stock-client (sans gradlew local)
├── settings.gradle            # Projet multi-module Gradle
├── deploy.ps1                 # Script déploiement complet Windows PowerShell
├── cleanup.ps1                # Script nettoyage complet
├── stockInterface/
│   ├── build.gradle
│   └── src/main/proto/
│       └── stock.proto        # Contrat gRPC (GetProduct + ManageStock)
├── stockClient/               # Service 1 — REST API → gRPC Client
│   ├── build.gradle
│   └── src/main/java/com/example/stockClient/
│       ├── StockClientApplication.java
│       ├── service/StockGrpcService.java
│       ├── web/StockController.java
│       └── resources/application.properties
├── stockServer/               # Service 2 — gRPC Server + JPA
│   ├── build.gradle
│   └── src/main/java/com/example/stockServer/
│       ├── StockServerApplication.java
│       ├── Product.java
│       ├── ProductRepository.java
│       ├── StockServiceImpl.java
│       ├── HealthController.java
│       └── resources/application.properties
└── k8s/
    ├── postgres-secret.yaml   # POSTGRES_USER, PASSWORD, DB (Opaque)
    ├── postgres-storage.yaml  # PV hostPath /mnt/data + PVC 5Gi
    ├── postgres-deployment.yaml
    ├── stock-server.yaml      # Deployment + Service ClusterIP (9090+8081)
    ├── stock-client.yaml      # Deployment + Service NodePort :31280
    ├── ingress.yaml           # Nginx Ingress host=stock.info
    └── rbac.yaml              # ServiceAccount + Role + RoleBinding
```

---

## Prérequis

- Docker Desktop
- Minikube : https://minikube.sigs.k8s.io/docs/start
- kubectl
- Aucun JDK ni Gradle local requis (build dans Docker)

---

## Déploiement

### Option 1 — Script automatisé (recommandé)

```powershell
# Windows PowerShell
.\deploy.ps1
```

### Option 2 — Commandes manuelles

```powershell
# 1. Démarrer Minikube
minikube start
minikube addons enable ingress
minikube addons enable ingress-dns

# 2. Créer le répertoire hostPath (OBLIGATOIRE pour le PV PostgreSQL)
minikube ssh -- sudo mkdir -p /mnt/data

# 3. Pointer Docker sur Minikube
minikube -p minikube docker-env | Invoke-Expression   # Windows PowerShell
# eval $(minikube docker-env)                          # Linux / Mac

# 4. Build des images (depuis la racine du projet)
docker build -f Dockerfile.server -t stock-server:1 .
docker build -f Dockerfile.client -t stock-client:1 .

# 5. Déploiement Kubernetes (ordre strict)
kubectl apply -f k8s/postgres-secret.yaml
kubectl apply -f k8s/postgres-storage.yaml
kubectl apply -f k8s/postgres-deployment.yaml
kubectl wait --for=condition=ready pod -l app=postgres --timeout=120s
kubectl apply -f k8s/rbac.yaml
kubectl apply -f k8s/stock-server.yaml
kubectl apply -f k8s/stock-client.yaml
kubectl apply -f k8s/ingress.yaml
```

---

## Accès à l'application

### 1. Ajouter dans le fichier hosts

```
# Windows : C:\Windows\System32\drivers\etc\hosts  (ouvrir notepad en admin)
# Linux / Mac : /etc/hosts
127.0.0.1   stock.info
```

### 2. Activer le tunnel (laisser ce terminal ouvert)

```powershell
minikube tunnel
```

### 3. Accéder à l'application

```
http://stock.info/
```

---

## Tests API

```powershell
# Connectivité inter-service
curl http://stock.info/

# Ajouter du stock (gRPC streaming bidirectionnel ManageStock)
curl.exe -X POST "http://stock.info/api/stock/PROD001/add?quantity=100"
curl.exe -X POST "http://stock.info/api/stock/PROD002/add?quantity=50"

# Consulter un produit (gRPC appel unaire GetProduct)
curl.exe http://stock.info/api/stock/PROD001
# {"productId":"PROD001","name":"Produit-PROD001","quantity":100,"status":"AVAILABLE"}

# Retirer du stock
curl.exe -X POST "http://stock.info/api/stock/PROD001/remove?quantity=30"
curl.exe http://stock.info/api/stock/PROD001
# {"quantity":70,"status":"AVAILABLE"}

# Produit inexistant
curl.exe http://stock.info/api/stock/INCONNU
# {"quantity":0,"status":"NOT_FOUND"}
```

> Sur Windows PowerShell, utiliser `curl.exe` (et non `curl`).

---

## Vérifications Kubernetes

```powershell
# Pods (tous Running 1/1)
kubectl get pods

# Services
kubectl get svc -o wide

# PVC (doit être Bound)
kubectl get pvc

# Secret (valeurs masquées)
kubectl describe secret postgres-secret

# Ingress
kubectl describe ingress stock-ingress

# RBAC — principe du moindre privilège
kubectl auth can-i get pods    --as=system:serviceaccount:default:stock-service-account  # yes
kubectl auth can-i delete pods --as=system:serviceaccount:default:stock-service-account  # no
```

---

## Nettoyage complet

```powershell
.\cleanup.ps1
```

---

## Barème

| Critère                   | Implémentation                                    | Note     |
|---------------------------|---------------------------------------------------|----------|
| 1 service + Docker + K8s  | stock-client + Dockerfile multi-stage + Deployment| 10/20    |
| Ingress gateway           | Nginx Ingress — host: stock.info                  | 12/20    |
| 2ème service relié        | stock-server gRPC + communication inter-service   | 14/20    |
| Base de données + PVC     | PostgreSQL 15 + PVC 5Gi (hostPath Minikube)       | 16/20    |
| RBAC + Secrets            | stock-service-account + postgres-secret Opaque    | 18/20    |
| gRPC (bonus)              | GetProduct (unaire) + ManageStock (streaming)     | +bonus   |

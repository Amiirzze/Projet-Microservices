# 📦 Gestion de Stock — Infrastructure Kubernetes + gRPC

Ce projet est une application de gestion de stock déployée sur un cluster Kubernetes local (**Minikube**). Il permet de gérer des produits (ajout, retrait, consultation de stock) via une architecture micro-services communicant en **gRPC / Protocol Buffers**, exposée au client HTTP via un **Nginx Ingress Controller**.

**Binôme :** AGAG Abdenour · DOUDOU Amir — Master RSA 2025/2026

---

## Architecture

- **Service 1 (stock-client)** : Spring Boot — API REST, gateway HTTP vers gRPC.
- **Service 2 (stock-server)** : Spring Boot — gRPC Server, logique métier + JPA.
- **Base de données** : PostgreSQL 15 pour la persistance (PersistentVolume 5Gi).
- **Ingress** : Nginx Ingress Controller (Gateway, host: `stock.info`).
- **Sécurité** : RBAC Kubernetes (moindre privilège) + Secret Opaque pour les credentials.

Voici le schéma de flux du projet :

```plaintext
┌─────────────────────────────────────────────────────────────┐
│                    KUBERNETES (Minikube)                    │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐    │
│  │         NGINX Ingress Controller (Gateway)          │    │
│  │  ┌───────────────────────────────────────────────┐  │    │
│  │  │ Routage :                                     │  │    │
│  │  │ - stock.info / → stock-client-service (:8080) │  │    │
│  │  └───────────────────────────────────────────────┘  │    │
│  └─────────────────────────────────────────────────────┘    │
│                          │                                  │
│                          ▼                                  │
│               ┌──────────────────────┐                      │
│               │  stock-client Pod    │  ← Service 1         │
│               │  Spring Boot :8080   │    API REST          │
│               └──────────┬───────────┘                      │
│                          │                                  │
│          gRPC / HTTP2 (:9090)                               │
│          • GetProduct  (appel unaire)                       │
│          • ManageStock (streaming bidirectionnel)           │
│                          │                                  │
│                          ▼                                  │
│               ┌──────────────────────┐                      │
│               │  stock-server Pod    │  ← Service 2         │
│               │  Spring Boot :9090   │    gRPC Server + JPA │
│               └──────────┬───────────┘                      │
│                          │                                  │
│                   JDBC/JPA (:5432)                          │
│                          │                                  │
│                          ▼                                  │
│               ┌──────────────────────┐                      │
│               │    PostgreSQL 15     │                      │
│               │      port 5432       │                      │
│               │  + PersistentVol 5Gi │                      │
│               └──────────────────────┘                      │
│                                                             │
│  [ RBAC : stock-service-account — lecture seule ]           │
│  [ Secret Opaque : credentials PostgreSQL chiffrés ]        │
└─────────────────────────────────────────────────────────────┘
```

---

## Structure du projet

```
stock-final/
├── Dockerfile.server              # Build multi-stage stock-server
├── Dockerfile.client              # Build multi-stage stock-client
├── settings.gradle                # Projet multi-module Gradle
├── deploy.ps1                     # Script déploiement complet
├── cleanup.ps1                    # Script nettoyage complet
├── stockInterface/
│   └── src/main/proto/
│       └── stock.proto            # Contrat gRPC 
├── stockClient/                   # Service 1 — REST API + gRPC Client
│   └── src/main/java/.../
│       ├── StockController.java
│       └── StockGrpcService.java
├── stockServer/                   # Service 2 — gRPC Server + JPA
│   └── src/main/java/.../
│       ├── StockServiceImpl.java
│       ├── Product.java
│       └── ProductRepository.java
└── k8s/
    ├── postgres-secret.yaml       # Secret Opaque 
    ├── postgres-storage.yaml      # PV hostPath + PVC 5Gi
    ├── postgres-deployment.yaml   # Déploiement PostgreSQL + Service
    ├── rbac.yaml                  # ServiceAccount + Role + RoleBinding
    ├── stock-server.yaml          # Déploiement stock-server + Service
    ├── stock-client.yaml          # Déploiement stock-client + Service
    └── ingress.yaml               # Nginx Ingress host=stock.info
```

---

## Guide de déploiement

- [Lancement Rapide](#lancement-rapide)
- [Lancement Manuel](#lancement-manuel)

---

### Lancement Rapide

#### 1. Déploiement complet



Sur Windows (PowerShell) :

```powershell
.\deploy.ps1
```

> **Note importante :** Une fois le déploiement terminé, ouvrez un terminal dédié et lancez :
> ```powershell
> minikube tunnel
> ```
> Puis accédez à l'application via `http://stock.info/`.

#### 2. Nettoyage (garde la persistance)



Sur Windows (PowerShell) :

```powershell
.\cleanup.ps1
```

---

### Lancement Manuel

#### 1. Prérequis



- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Minikube](https://minikube.sigs.k8s.io/docs/start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)



#### 2. Démarrage de l'environnement

```powershell
minikube start
minikube addons enable ingress
minikube addons enable ingress-dns
```

#### 3. Créer le répertoire hostPath (obligatoire pour PostgreSQL)

```powershell
minikube ssh -- sudo mkdir -p /mnt/data
```

#### 4. Préparation des images Docker (locale)



Sur Windows (PowerShell) :

```powershell
minikube -p minikube docker-env | Invoke-Expression
```

Sur Linux / macOS :

```bash
eval $(minikube docker-env)
```

Ensuite, buildez les images :

```powershell
# Build du Service 2 (gRPC Server)
docker build -f Dockerfile.server -t stock-server:1 .

# Build du Service 1 (REST API)
docker build -f Dockerfile.client -t stock-client:1 .
```

#### 5. Déploiement des micro-services (ordre strict)

```powershell
kubectl apply -f k8s/postgres-secret.yaml
kubectl apply -f k8s/postgres-storage.yaml
kubectl apply -f k8s/postgres-deployment.yaml
kubectl wait --for=condition=ready pod -l app=postgres --timeout=120s
kubectl apply -f k8s/rbac.yaml
kubectl apply -f k8s/stock-server.yaml
kubectl apply -f k8s/stock-client.yaml
kubectl apply -f k8s/ingress.yaml
```

#### 6. Accès à l'application

Ajoutez dans votre fichier `hosts` :

```
# Windows : C:\Windows\System32\drivers\etc\hosts (ouvrir Notepad en admin)
# Linux / macOS : /etc/hosts
127.0.0.1   stock.info
```

Puis dans un terminal dédié (laisser ouvert) :

```powershell
minikube tunnel
```

Accès final : [http://stock.info/](http://stock.info/)

---

## Tests API

```powershell
# Connectivité inter-service
curl http://stock.info/

# Ajouter du stock (via gRPC ManageStock — streaming bidirectionnel)
curl.exe -X POST "http://stock.info/api/stock/PROD001/add?quantity=100"
# Operation ADD envoyee pour PROD001 (100 unites)

curl.exe -X POST "http://stock.info/api/stock/PROD002/add?quantity=50"
# Operation ADD envoyee pour PROD002 (50 unites)

# Consulter un produit (via gRPC GetProduct — appel unaire)
curl.exe http://stock.info/api/stock/PROD001
# {"productId":"PROD001","name":"Produit-PROD001","quantity":100,"status":"AVAILABLE"}

# Retirer du stock
curl.exe -X POST "http://stock.info/api/stock/PROD001/remove?quantity=30"
curl.exe http://stock.info/api/stock/PROD001
# {"productId":"PROD001","quantity":70,"status":"AVAILABLE"}

# Produit inexistant
curl.exe http://stock.info/api/stock/INCONNU
# {"productId":"INCONNU","quantity":0,"status":"NOT_FOUND"}
```


## Vérifications Kubernetes

```powershell
# Pods (tous Running 1/1)
kubectl get pods

# Services et ports
kubectl get svc -o wide

# PVC (doit être Bound, 5Gi)
kubectl get pvc

# Secret (valeurs masquées)
kubectl describe secret postgres-secret

# Ingress
kubectl describe ingress stock-ingress

# RBAC — validation du principe du moindre privilège
kubectl auth can-i get pods    --as=system:serviceaccount:default:stock-service-account  # yes
kubectl auth can-i delete pods --as=system:serviceaccount:default:stock-service-account  # no




Write-Host "=== 1. Demarrage Minikube ===" -ForegroundColor Cyan
minikube start
minikube addons enable ingress
minikube addons enable ingress-dns

Write-Host "=== 2. Creation du repertoire hostPath pour PostgreSQL ===" -ForegroundColor Cyan
minikube ssh -- sudo mkdir -p /mnt/data

Write-Host "=== 3. Configuration Docker -> Minikube ===" -ForegroundColor Cyan
minikube -p minikube docker-env | Invoke-Expression

Write-Host "=== 4. Build image stock-server ===" -ForegroundColor Cyan
docker build -f Dockerfile.server -t stock-server:1 .

Write-Host "=== 5. Build image stock-client ===" -ForegroundColor Cyan
docker build -f Dockerfile.client -t stock-client:1 .

Write-Host "=== 6. Deploiement Kubernetes ===" -ForegroundColor Cyan
kubectl apply -f k8s/postgres-secret.yaml
kubectl apply -f k8s/postgres-storage.yaml
kubectl apply -f k8s/postgres-deployment.yaml

Write-Host "--- Attente PostgreSQL (120s max) ---" -ForegroundColor Yellow
kubectl wait --for=condition=ready pod -l app=postgres --timeout=120s

kubectl apply -f k8s/rbac.yaml
kubectl apply -f k8s/stock-server.yaml
kubectl apply -f k8s/stock-client.yaml
kubectl apply -f k8s/ingress.yaml

Write-Host "=== 7. Verification ===" -ForegroundColor Cyan
kubectl get pods
kubectl get svc -o wide
kubectl get ingress
kubectl get pvc

Write-Host "=== DONE - Lancer minikube tunnel dans un autre terminal ===" -ForegroundColor Green
Write-Host "=== Puis acceder a http://stock.info/ ===" -ForegroundColor Green

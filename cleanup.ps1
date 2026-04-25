kubectl delete -f k8s/
kubectl delete pvc postgres-pv-claim
kubectl delete pv  postgres-pv-volume
minikube ssh -- sudo rm -rf /mnt/data
Write-Host "Environnement nettoye." -ForegroundColor Green

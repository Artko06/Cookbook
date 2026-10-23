# Victoria Metrics Single в minikube

Victoria Metrics устанавливается Helm-чартом `vm/victoria-metrics-single` в namespace `monitoring`.

## Установка

```bash
helm repo add vm https://victoriametrics.github.io/helm-charts/
helm repo update

helm upgrade --install vm-single vm/victoria-metrics-single \
  -n monitoring --create-namespace \
  -f k8s/victoria-metrics/values.yaml
```

## Доступ

```bash
kubectl port-forward svc/vm-single-victoria-metrics-single-server -n monitoring 8428:8428
```

```text
http://localhost:8428/vmui/
```

## Удаление

```bash
helm uninstall vm-single -n monitoring
```


#!/usr/bin/env bash
# Шлёт непрерывный трафик через Envoy-прокси и печатает версию и имя пода,
# которые ответили. Так видно, как стратегия распределяет запросы.
#
# Использование:
#   ./traffic.sh            # бесконечно
#   ./traffic.sh 100        # 100 запросов и выйти
#   RATE=0.2 ./traffic.sh   # интервал между запросами, сек
#   EXTRA_HEADER='x-user-group: beta' ./traffic.sh 20  # для A/B
set -euo pipefail

NS="${NS:-cookbook-deploy}"
GATEWAY_NAME="${GATEWAY_NAME:-eg}"
EG_NAMESPACE="${EG_NAMESPACE:-envoy-gateway-system}"
LOCAL_PORT="${LOCAL_PORT:-8888}"
RATE="${RATE:-0.2}"
COUNT="${1:-0}"
TARGET="${TARGET:-/api/v1/recipes?size=1}"
EXTRA_HEADER="${EXTRA_HEADER:-}"

# --- находим Envoy-прокси для Gateway --------------------------------------
# Сначала ищем по имени и namespace Gateway, чтобы не перепутать с demo Gateway `eg`
# из namespace deploy-demo. Если метки namespace нет — откатываемся к имени.
SVC="$(kubectl get svc -n "$EG_NAMESPACE" \
    -l "gateway.envoyproxy.io/owning-gateway-name=$GATEWAY_NAME,gateway.envoyproxy.io/owning-gateway-namespace=$NS" \
    -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)"
if [[ -z "$SVC" ]]; then
    SVC="$(kubectl get svc -n "$EG_NAMESPACE" \
        -l "gateway.envoyproxy.io/owning-gateway-name=$GATEWAY_NAME" \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)"
fi
if [[ -z "$SVC" ]]; then
    echo "Не найден Envoy-прокси для Gateway '$GATEWAY_NAME' в namespace $EG_NAMESPACE." >&2
    echo "Проверьте, что setup.sh отработал и Gateway запрограммирован:" >&2
    echo "  kubectl get gateway -n $NS $GATEWAY_NAME" >&2
    exit 1
fi

PF_LOG="$(mktemp)"
HEADERS="$(mktemp)"
BODY="$(mktemp)"
PF_PID=""
cleanup() {
    if [[ -n "$PF_PID" ]]; then
        kill "$PF_PID" 2>/dev/null || true
    fi
    rm -f "$PF_LOG" "$HEADERS" "$BODY"
}
trap cleanup EXIT
trap 'exit 130' INT TERM

echo "Пробрасываю :$LOCAL_PORT -> $EG_NAMESPACE/svc/$SVC:80"
kubectl port-forward -n "$EG_NAMESPACE" "svc/$SVC" "${LOCAL_PORT}:80" \
    >"$PF_LOG" 2>&1 &
PF_PID=$!

# ждём, пока порт откроется
for _ in $(seq 1 30); do
    if ! kill -0 "$PF_PID" 2>/dev/null; then
        cat "$PF_LOG" >&2
        exit 1
    fi
    if curl -s -o /dev/null "http://localhost:${LOCAL_PORT}/" 2>/dev/null; then
        break
    fi
    sleep 0.5
done

URL="http://localhost:${LOCAL_PORT}${TARGET}"
CURL_ARGS=(-sS)
if [[ -n "$EXTRA_HEADER" ]]; then
    CURL_ARGS+=(-H "$EXTRA_HEADER")
fi

echo "Трафик: $URL (интервал ${RATE}s). Ctrl-C — остановить."
echo "-------------------------------------------------------------------"

i=0
while :; do
    : > "$HEADERS"
    if ! status="$(curl "${CURL_ARGS[@]}" -o "$BODY" -D "$HEADERS" \
        -w '%{http_code}' "$URL" 2>/dev/null)"; then
        status="000"
    fi
    version="$(awk 'tolower($1) == "x-app-version:" {gsub(/\r/, "", $2); print $2; exit}' "$HEADERS")"
    instance="$(awk 'tolower($1) == "x-app-instance:" {gsub(/\r/, "", $2); print $2; exit}' "$HEADERS")"
    [[ -n "$version" ]] || version="-"
    [[ -n "$instance" ]] || instance="-"
    printf '%s | %s | %-7s | %s\n' "$(date +%H:%M:%S)" "$status" "$version" "$instance"
    i=$((i + 1))
    if [[ "$COUNT" -gt 0 && "$i" -ge "$COUNT" ]]; then
        break
    fi
    sleep "$RATE"
done

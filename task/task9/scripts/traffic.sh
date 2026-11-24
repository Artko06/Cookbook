#!/usr/bin/env bash
# Отправляет запросы через Envoy Gateway и считает ответы по версиям.
#
# Использование:
#   ./traffic.sh                 # 200 запросов, пауза 0.05 с
#   ./traffic.sh 150             # 150 запросов
#   COUNT=100 RATE=0 ./traffic.sh
#   TARGET=/api/v1/recipes ./traffic.sh
#
# Сырой лог и итоговая таблица сохраняются в task/task9/results/.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${RESULTS_DIR:-$SCRIPT_DIR/../results}"

NS="${NS:-cookbook-deploy}"
GATEWAY_NAME="${GATEWAY_NAME:-eg}"
EG_NAMESPACE="${EG_NAMESPACE:-envoy-gateway-system}"
LOCAL_PORT="${LOCAL_PORT:-8888}"
RATE="${RATE:-0.05}"
COUNT="${1:-${COUNT:-200}}"
TARGET="${TARGET:-/api/v1/recipes?size=1}"

die() { printf '\033[1;31mОШИБКА: %s\033[0m\n' "$*" >&2; exit 1; }

[[ "$COUNT" =~ ^[0-9]+$ ]] && (( COUNT > 0 )) \
    || die "COUNT должен быть положительным целым числом"

command -v kubectl >/dev/null 2>&1 || die "не найден kubectl"
command -v curl >/dev/null 2>&1 || die "не найден curl"
[[ "$(kubectl config current-context 2>/dev/null)" == "minikube" ]] \
    || die "текущий Kubernetes-контекст не minikube"

mkdir -p "$RESULTS_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
RAW_LOG="$RESULTS_DIR/traffic-$STAMP.log"
SUMMARY="$RESULTS_DIR/summary-$STAMP.txt"

# --- находим Envoy-прокси для Gateway --------------------------------------
# Сначала по имени и namespace Gateway, затем запасной вариант — только по имени.
SVC="$(kubectl get svc -n "$EG_NAMESPACE" \
    -l "gateway.envoyproxy.io/owning-gateway-name=$GATEWAY_NAME,gateway.envoyproxy.io/owning-gateway-namespace=$NS" \
    -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)"
if [[ -z "$SVC" ]]; then
    SVC="$(kubectl get svc -n "$EG_NAMESPACE" \
        -l "gateway.envoyproxy.io/owning-gateway-name=$GATEWAY_NAME" \
        -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)"
fi
[[ -n "$SVC" ]] || die "не найден Envoy-прокси для Gateway '$GATEWAY_NAME' в namespace $EG_NAMESPACE"

PF_LOG="$(mktemp)"
HEADERS="$(mktemp)"
PF_PID=""
cleanup() {
    if [[ -n "$PF_PID" ]]; then
        kill "$PF_PID" 2>/dev/null || true
    fi
    rm -f "$PF_LOG" "$HEADERS"
}
trap cleanup EXIT
trap 'exit 130' INT TERM

printf 'Пробрасываю localhost:%s -> %s/svc/%s:80\n' "$LOCAL_PORT" "$EG_NAMESPACE" "$SVC"
kubectl port-forward -n "$EG_NAMESPACE" "svc/$SVC" "$LOCAL_PORT:80" \
    >"$PF_LOG" 2>&1 &
PF_PID=$!

for _ in $(seq 1 30); do
    if ! kill -0 "$PF_PID" 2>/dev/null; then
        cat "$PF_LOG" >&2
        die "port-forward завершился"
    fi
    if curl -s -o /dev/null "http://localhost:${LOCAL_PORT}/" 2>/dev/null; then
        break
    fi
    sleep 0.5
done

URL="http://localhost:${LOCAL_PORT}${TARGET}"
printf 'URL: %s\n' "$URL"
printf 'Запросов: %s, пауза: %ss\n' "$COUNT" "$RATE"
printf 'Сырой лог: %s\n' "$RAW_LOG"
printf '%s\n' '-------------------------------------------------------------------'

declare -A counts=()
declare -A statuses=()
total=0
printf '# time\tstatus\tversion\tinstance\n' >"$RAW_LOG"

for i in $(seq 1 "$COUNT"); do
    : >"$HEADERS"
    if ! status="$(curl -sS -o /dev/null -D "$HEADERS" -w '%{http_code}' "$URL" 2>/dev/null)"; then
        status="000"
    fi

    version="$(awk 'tolower($1) == "x-app-version:" {gsub(/\r/, "", $2); print $2; exit}' "$HEADERS")"
    instance="$(awk 'tolower($1) == "x-app-instance:" {gsub(/\r/, "", $2); print $2; exit}' "$HEADERS")"
    [[ -n "$version" ]] || version="unknown"
    [[ -n "$instance" ]] || instance="-"

    printf '%s\t%s\t%s\t%s\n' "$(date +%H:%M:%S)" "$status" "$version" "$instance" >>"$RAW_LOG"
    counts["$version"]=$(( ${counts["$version"]:-0} + 1 ))
    statuses["$status"]=$(( ${statuses["$status"]:-0} + 1 ))
    total=$((total + 1))

    if (( i % 25 == 0 || i == COUNT )); then
        printf '\rОтправлено: %d/%d' "$i" "$COUNT"
    fi
    sleep "$RATE"
done
printf '\n'

{
    printf 'Task9: распределение трафика 90/10\n'
    printf 'Дата: %s\n' "$(date '+%Y-%m-%d %H:%M:%S')"
    printf 'URL: %s\n' "$URL"
    printf 'Всего запросов: %d\n\n' "$total"

    printf '  Версия      Ответов      Доля\n'
    while IFS= read -r version; do
        c="${counts[$version]}"
        pct="$(awk -v c="$c" -v t="$total" 'BEGIN { printf "%.1f", c * 100 / t }')"
        printf '  %-10s %8d %8s%%\n' "$version" "$c" "$pct"
    done < <(printf '%s\n' "${!counts[@]}" | sort)

    printf '\nHTTP-статусы:\n'
    while IFS= read -r st; do
        printf '  %-5s %d\n' "$st" "${statuses[$st]}"
    done < <(printf '%s\n' "${!statuses[@]}" | sort)

    printf '\nОжидаемо при весах 90/10: v1.0.0 ~90%%, v2.0.0 ~10%%.\n'
} | tee "$SUMMARY"

printf '\nИтог сохранён: %s\n' "$SUMMARY"

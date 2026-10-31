#!/usr/bin/env python3

import json
import logging
import os
import threading
import time
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

PROBE_URL = os.getenv("PROBE_URL", "http://app:8080/api/v1/recipes?size=5")
PROBE_INTERVAL_SECONDS = float(os.getenv("PROBE_INTERVAL_SECONDS", "15"))
PROBE_TIMEOUT_SECONDS = float(os.getenv("PROBE_TIMEOUT_SECONDS", "5"))
PROBE_PORT = int(os.getenv("PROBE_PORT", "9090"))

logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO"),
    format="%(asctime)s %(levelname)s %(message)s",
)
log = logging.getLogger("prober")


class Metrics:
    def __init__(self):
        self._lock = threading.Lock()
        self._success = 0
        self._failure = 0
        self._duration = 0.0
        self._up = 1

    def success(self, duration):
        with self._lock:
            self._success += 1
            self._duration = duration
            self._up = 1

    def failure(self, duration):
        with self._lock:
            self._failure += 1
            self._duration = duration
            self._up = 1

    def mark_down(self):
        with self._lock:
            self._up = 0

    def render(self):
        with self._lock:
            return (
                "# HELP cookbook_probe_runs_total Total number of probe runs.\n"
                "# TYPE cookbook_probe_runs_total counter\n"
                f'cookbook_probe_runs_total{{result="success"}} {self._success}\n'
                f'cookbook_probe_runs_total{{result="failure"}} {self._failure}\n'
                "# HELP cookbook_probe_duration_seconds Duration of the last probe run.\n"
                "# TYPE cookbook_probe_duration_seconds gauge\n"
                f"cookbook_probe_duration_seconds {self._duration:.6f}\n"
                "# HELP cookbook_probe_up Whether the prober loop is running.\n"
                "# TYPE cookbook_probe_up gauge\n"
                f"cookbook_probe_up {self._up}\n"
            )


metrics = Metrics()


def run_probe():
    started = time.monotonic()
    try:
        request = urllib.request.Request(PROBE_URL, headers={"User-Agent": "cookbook-prober/1.0"})
        with urllib.request.urlopen(request, timeout=PROBE_TIMEOUT_SECONDS) as response:
            body = response.read()
            if not 200 <= response.status < 300:
                raise RuntimeError(f"unexpected status {response.status}")
            json.loads(body)
        duration = time.monotonic() - started
        metrics.success(duration)
        log.info("probe success duration=%.3fs", duration)
    except (OSError, ValueError, RuntimeError, urllib.error.URLError) as exc:
        duration = time.monotonic() - started
        metrics.failure(duration)
        log.warning("probe failure duration=%.3fs error=%s", duration, exc)


def probe_loop():
    while True:
        try:
            run_probe()
        except Exception:
            metrics.mark_down()
            log.exception("unexpected prober error")
        time.sleep(PROBE_INTERVAL_SECONDS)


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/metrics":
            self._respond(200, metrics.render().encode())
        elif self.path == "/healthz":
            self._respond(200, b"ok\n")
        else:
            self._respond(404, b"not found\n")

    def _respond(self, status, body):
        self.send_response(status)
        self.send_header("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        log.debug("http " + format, *args)


def main():
    threading.Thread(target=probe_loop, daemon=True).start()
    server = ThreadingHTTPServer(("0.0.0.0", PROBE_PORT), Handler)
    log.info("prober listening on :%s, target=%s", PROBE_PORT, PROBE_URL)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()

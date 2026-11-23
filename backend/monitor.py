"""Pilot TCP monitoring API. Bind to loopback behind an authenticated TLS reverse proxy.

Targets come only from administrator-owned configuration, never from API requests.
"""
import hmac
import ipaddress
import json
import os
import socket
import threading
import time
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

CONFIG = Path(os.environ.get("INFRAGUARD_TARGETS_FILE", "targets.json"))
TOKEN = os.environ.get("INFRAGUARD_MONITOR_TOKEN", "")
INTERVAL = int(os.environ.get("INFRAGUARD_INTERVAL_SECONDS", "30"))


def load_targets(path):
    raw = json.loads(Path(path).read_text())
    if not isinstance(raw, list) or not raw or len(raw) > 100:
        raise ValueError("Configure between 1 and 100 approved targets")
    seen = set()
    for target in raw:
        if set(target) != {"id", "name", "address", "port"}:
            raise ValueError("Each target needs id, name, address, port only")
        if not isinstance(target["id"], str) or not target["id"] or target["id"] in seen:
            raise ValueError("Target IDs must be unique nonempty strings")
        seen.add(target["id"])
        # Literal IPs avoid DNS rebinding and prevent an API caller supplying destinations.
        ipaddress.ip_address(target["address"])
        if type(target["port"]) is not int or not 1 <= target["port"] <= 65535:
            raise ValueError("Invalid TCP port")
        if not isinstance(target["name"], str) or not target["name"].strip():
            raise ValueError("Target name required")
    return raw


def probe(target, timeout=2):
    start = time.monotonic()
    try:
        with socket.create_connection((target["address"], target["port"]), timeout=timeout):
            status = "Online"
            latency = round((time.monotonic() - start) * 1000)
    except (OSError, TimeoutError):
        status, latency = "Offline", None
    return {**target, "status": status, "latencyMs": latency,
            "checkedAt": datetime.now(timezone.utc).isoformat()}


class Monitor:
    def __init__(self, targets, interval):
        self.targets, self.interval = targets, interval
        self.snapshot = []
        self.lock = threading.Lock()
        self.stop = threading.Event()

    def run(self):
        while not self.stop.is_set():
            # Only publish a complete cycle; stale readings are detectable via checkedAt.
            results = [probe(target) for target in self.targets]
            with self.lock:
                self.snapshot = results
            self.stop.wait(self.interval)

    def readings(self):
        with self.lock:
            return list(self.snapshot)


def handler_for(monitor, token):
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            if self.path != "/v1/monitoring/devices":
                return self.reply(404, {"error": "Not found"})
            supplied = self.headers.get("Authorization", "")
            if not hmac.compare_digest(supplied, "Bearer " + token):
                return self.reply(401, {"error": "Unauthorized"})
            return self.reply(200, {"devices": monitor.readings(), "source": "tcp-connect"}, no_cache=True)

        def reply(self, code, payload, no_cache=False):
            data = json.dumps(payload).encode()
            self.send_response(code)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(data)

        def log_message(self, format, *args):
            # Never log Authorization headers.
            super().log_message(format, *args)

    return Handler


def main():
    if len(TOKEN) < 32:
        raise SystemExit("INFRAGUARD_MONITOR_TOKEN must be a random secret of at least 32 characters")
    if not 5 <= INTERVAL <= 3600:
        raise SystemExit("Interval must be between 5 and 3600 seconds")
    targets = load_targets(CONFIG)
    monitor = Monitor(targets, INTERVAL)
    threading.Thread(target=monitor.run, daemon=True).start()
    # Loopback only: expose to the phone via a TLS proxy/VPN with access controls.
    server = ThreadingHTTPServer(("127.0.0.1", int(os.environ.get("INFRAGUARD_PORT", "8080"))), handler_for(monitor, TOKEN))
    server.serve_forever()


if __name__ == "__main__":
    main()

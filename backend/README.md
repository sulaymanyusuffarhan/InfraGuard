# Pilot monitoring service (TCP only)

This is a real, read-only TCP connectivity collector, **not** a full NMS or production identity service. It checks only administrator-configured literal IP:port pairs, in order, every 30 seconds. "Online" means a TCP handshake succeeded, not that the application is healthy. The checked timestamp is UTC; no historical uptime, alerting, ICMP, SNMP, CPU or RAM collection is implemented. An empty response means the first polling cycle has not completed.

## Run on a server with access to approved targets

```sh
cp backend/targets.example.json backend/targets.json
# Edit targets.json with approved TBC addresses and ports; ensure monitoring is authorized.
export INFRAGUARD_TARGETS_FILE="$PWD/backend/targets.json"
export INFRAGUARD_MONITOR_TOKEN="$(python3 -c 'import secrets; print(secrets.token_urlsafe(48))')"
python3 -m backend.monitor
```

The server binds to **127.0.0.1:8080**. Put it behind an organization-managed HTTPS reverse proxy reachable only via corporate VPN, with TLS certificates and request limits; do not publish port 8080 or the bearer token. Configure the proxy to forward only `/v1/monitoring/devices`, never redirect API requests, and add production identity/role enforcement before wider deployment. Enter the proxy's HTTPS origin and token on the Android Infrastructure tab. Token is held in memory, not persisted. A new token is required after process restart/logout.

Test locally: `python3 -m unittest discover -s backend/tests -v`. Example targets use documentation-only IP addresses and must be replaced. Never commit the actual targets file or real tokens.

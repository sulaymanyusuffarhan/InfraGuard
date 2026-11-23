# Security and limitations

This app has no organization login. While foregrounded, it automatically performs bounded checks against private IPv4 addresses on the phone's active Wi-Fi subnet (/24–/30, max 254 targets; TCP 22, 80, 443; 20 concurrent probes). Use it only on networks you are authorized to inspect. No background scan runs while the app is stopped. No arbitrary user-supplied scanning target is accepted.

Responsive IP/port observations are stored in app-private SQLite; no observations are uploaded. Records can be deleted per subnet from Infrastructure. Android backups are disabled. No location permission is requested, so SSID is unavailable; subnet/IP is not a unique Wi-Fi identity. Old records do not prove that a host remains online, and a lack of response does not prove a host is offline. Wired hosts may be visible too.

The optional backend monitoring pilot uses a separately entered static token and is **not** production identity; see `backend/README.md`. Security, audit, patch, SSH and vulnerability functions are not operational. Any future privileged functions require authentication, server-side authorization, audit logging and security testing. No credentials are embedded in source.

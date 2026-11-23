# Security posture and limitations

**Not production-ready.** Demo role selection is not login. All data is fabricated. Buttons marked unavailable do not send requests. Client-side checks only improve UX; the backend must enforce every permission, ownership rule, approval and audit record.

Before production: OIDC PKCE and managed SSO; Android Keystore-backed token storage and biometric step-up for privileged operations; enforce TLS, pinning policy with rotation plan, certificate validation, token revocation, idle timeout and device integrity policy. Reject unauthorized scope on every backend route; separate approver from requester. Never store SSH secrets on device. All scans need documented approval and server-side target allowlists. All patch changes require approvals, safe maintenance windows, rollback plans and emergency abort. Audit events are append-only, timestamped and retained by policy. Redact logs, evidence and notification payloads; limit cache lifetime; disable backups for sensitive data. Conduct OWASP MASVS review, penetration testing, dependency scanning and incident recovery drills before deployment.

No real credentials, network permissions, production endpoints or secret material are included in this repository.

## Monitoring pilot limitations
The pilot uses one static bearer secret configured in server environment and manually entered on the phone. It has no user identity, per-device permissions, token rotation endpoint, audit trail, secret vault or rate limiting; it must remain on a restricted internal network behind a TLS/VPN proxy. The Android app does not persist the token and rejects non-HTTPS or redirected API requests. A connected pilot does **not** authenticate the user to other modules. The overview and other modules remain sample data.

## Local Wi-Fi mode
No organization sign-in is required for local Wi-Fi details or manual discovery. The app can only see what the phone can access on its connected Wi-Fi network; users must have permission to inspect that network. Scans are never automatic and are limited in scope/concurrency. No SSID/location permission is requested. Sample roles are not access controls. Do not use this mode as a security assessment or inventory source of truth.

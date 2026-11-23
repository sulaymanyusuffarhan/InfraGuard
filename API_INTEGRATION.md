# Backend integration contract (proposed, not implemented)

Base URL is environment-specific HTTPS (`.env.example` is a template, not read by the app). All endpoints require OIDC access tokens and backend authorization scoped to department, assigned asset and action. Pagination, timestamp (UTC ISO-8601), correlation ID, idempotency key for mutations, typed errors and schema version are required. Admin web uses the same API with separate client registration.

| Resource | Proposed endpoints | Enforcement |
|---|---|---|
| Dashboard | `GET /overview`, `GET /metrics?range=` | Aggregate permitted scope only |
| Devices | `GET /devices`, `GET /devices/{id}/health`, `GET /alerts`, `POST /alerts/{id}/ack` | Backend owns probes, dependency graph and escalation |
| Assets | `GET/POST /assets`, `GET/PATCH /assets/{id}`, `GET /assets/{id}/history` | Assignment/retirement audited |
| Inspection | `POST /assets/{id}/inspection-requests`, `GET /assets/{id}/inspections` | Agent-only execution, scope approval |
| Vulnerabilities | `GET /findings`, `POST /scan-requests`, `POST /findings/{id}/verify` | Approved target allowlist, rate limits |
| Incidents | `GET/POST /incidents`, `PATCH /incidents/{id}`, `POST /incidents/{id}/evidence` | Malware-safe attachment handling |
| Audit | `GET /audit-events` | Separate read privilege, redaction |
| Patches | `GET /patches`, `POST /rollouts`, `POST /rollouts/{id}/approve`, `POST /rollouts/{id}/abort` | Step-up, staged approvals, immutable audit |
| Changes | `GET/POST /changes`, `POST /changes/{id}/approve` | Separation of duties |
| Remote | `POST /access-requests`, `POST /sessions` | Short-lived brokered credentials, session recording |
| Helpdesk | `GET/POST /tickets`, `PATCH /tickets/{id}` | Employee/technician scopes |
| Reports | `POST /reports`, `GET /reports/{id}/download` | Server-side redaction and signed short-lived download |

Streams: `GET /events` via authenticated SSE or WebSocket with resumption cursor. Push messages carry opaque references, not confidential details. Backend integrates SNMP collectors, SIEM, EDR/agents, Tripwire-equivalent FIM, scanners, patch manager and FCM. API must never expose collectors directly to the public internet.

## Implemented pilot endpoint
`GET /v1/monitoring/devices` requires `Authorization: Bearer <monitor-token>` and returns `{ "source": "tcp-connect", "devices": [{ "id", "name", "address", "port", "status": "Online|Offline", "latencyMs": number|null, "checkedAt": "UTC ISO-8601" }] }`. Targets are server-configured. This is a limited pilot contract, not the proposed full backend contract above.

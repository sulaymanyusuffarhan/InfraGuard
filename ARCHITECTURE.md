# Architecture and phased delivery

## Repository assessment
The initial repository contained only a one-line README. No existing application or backend was available to preserve.

## Current implementation
Single `:app` module. `Domain.kt` contains models, role capability mapping, repository interface and in-memory demonstration implementation. `MainActivity.kt` contains Compose screens and a ViewModel holding session-scoped demo state. `StateFlow` feeds Compose. Demo roles are explicitly not authentication or authorization. Data is ephemeral; no network permission is requested.

## Production target
Split into `core:model`, `core:network`, `core:security`, `core:database`, and feature modules once backend contracts stabilize. Use OIDC authorization-code + PKCE with external browser, short-lived tokens protected by Android Keystore, encrypted storage, server-side RBAC/ABAC and step-up auth. Inject Retrofit/Ktor repositories via Hilt; use Room only for authorized, minimized offline caches, DataStore for non-secret preferences. Polling or authenticated WebSocket/SSE streams should be lifecycle-aware and back off on failure. Never connect the phone directly to SNMP, SSH or endpoint agents.

## Delivery roadmap
1. Foundation/demo UI (implemented); replace demo entry with real OIDC before production.
2. Monitoring adapter and dependency-aware alerting on backend; server and network health.
3. Asset ownership, agent-based inspection, scoped scan requests and remediation workflows.
4. Incidents, evidence upload, immutable audit and authorized logs; notification gateway.
5. Approved ONE/SOME/MANY patch rollout, change workflow and gateway-only remote access.
6. Server-generated reports, integration tests, accessibility review, threat modeling, signed APK and security assessment.

Missing services: identity, monitoring/SNMP collectors, endpoint agents, vulnerability scanners, centralized logging, patch orchestration, firewall/VPN gateway, helpdesk, push service, report generation, administrative web interface and immutable audit store.

The initial splash is a short Compose-branded transition using the supplied logo; it does not imply that a backend is loading or authenticated. Navigation selection derives from the Navigation back stack, so system Back and top-bar destinations stay consistent.

# TBC InfraGuard

Native Android operations console for the Tanzania Broadcasting Corporation IT department. **Current deliverable is a demonstration foundation, not a production monitoring or authentication system.** No live TBC infrastructure is contacted. In-memory sample records are labeled in the interface and disappear on process restart.

## Included
- Kotlin, Jetpack Compose, Material 3 light/dark, responsive scrolling, five-tab navigation and notification/profile screens.
- Seven demo roles with explicit client-side navigation permissions (not a security boundary).
- Searchable infrastructure and asset lists, incident and asset registration forms with validation, vulnerability sample queue, and local reactive repository state.
- Explicit integration-required screens for patching, change approvals, remote access, helpdesk and reporting. These do not execute operations.

See [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md), [ARCHITECTURE.md](ARCHITECTURE.md), [API_INTEGRATION.md](API_INTEGRATION.md), and [SECURITY.md](SECURITY.md).

## Brand asset
The splash and login screens use the supplied TBC-NMS shield artwork (`app/src/main/res/drawable-nodpi/tbc_nms_logo.webp`), resized for mobile delivery. Its embedded “TBC-NMS” wording is part of the supplied image; the surrounding product name remains **TBC InfraGuard**. Confirm artwork usage rights and final naming with TBC before distribution.

## Live monitoring pilot
Infrastructure can now show **real TCP reachability** via the bundled [monitoring service](backend/README.md). It requires approved targets, deployment on an internal server, an HTTPS/VPN proxy and a per-session token. The rest of the application (overview, security, assets and notifications) still uses visibly marked sample data. Do not interpret demo role selection as authentication or production authorization.

## On-device Wi-Fi view (no sign-in)
The app now opens directly into the dashboard. Infrastructure displays the active Wi-Fi IPv4 address and gateway without a backend, VPN or token. A user-initiated local TCP check can discover responding hosts on ports 22, 80 and 443 within a private IPv4 /24-or-smaller subnet (max 254 addresses). No automatic scan, full device enumeration, SSH access or vulnerability scan is performed. Android does not expose the SSID without additional location permission, which this app does not request. Other screens retain explicitly marked sample content. Server monitoring remains optional and separate.

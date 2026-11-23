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

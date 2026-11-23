# TBC InfraGuard

Native Android Wi-Fi discovery application built with Kotlin, Jetpack Compose and Material 3. No organization sign-in or backend is needed to see the **phone's active Wi-Fi IP/gateway** or to record responsive local hosts. The supplied TBC shield is used on the splash screen.

When the app is foregrounded and joins Wi-Fi, it checks hosts on its private IPv4 subnet (prefix /24–/30, up to 254 addresses). It checks only TCP ports 22, 80 and 443 with bounded concurrency and stores responsive IP addresses and ports locally. Overview shows the current check; Assets shows locally recorded addresses and last-seen times. You can recheck, stop discovery and delete records for the current subnet. No sample data or simulated alerts are shown.

**Important:** This is NOT a complete list of devices using Wi-Fi. A host with closed ports, a sleeping host, or a client isolated by the router will not appear. Hosts on Ethernet may appear if reachable on the same subnet. Local records are keyed by subnet/IP (not SSID), so different networks sharing an IP range may share records. An unlisted host is not proven offline. The app does not run background scans while closed, inspect host internals, or upload results.

Security, notification and remote management modules remain unavailable until real integrations exist. The optional separately deployed [TCP monitoring pilot](backend/README.md) remains available behind its own connection controls.

See [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md), [ARCHITECTURE.md](ARCHITECTURE.md), and [SECURITY.md](SECURITY.md).

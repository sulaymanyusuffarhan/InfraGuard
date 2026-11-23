package tz.go.tbc.infraguard

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class Permission { VIEW_INFRA, VIEW_SECURITY, VIEW_AUDIT, VIEW_ASSETS, REGISTER_ASSET, REPORT_INCIDENT, TRIAGE_INCIDENT, REQUEST_SCAN, APPROVE_CHANGE, EXPORT_REPORT }
enum class Role(val title: String, val permissions: Set<Permission>) {
    SYSTEM_ADMIN("System Administrator", Permission.entries.toSet()),
    NETWORK_ADMIN("Network Administrator", setOf(Permission.VIEW_INFRA, Permission.VIEW_ASSETS, Permission.REGISTER_ASSET, Permission.REQUEST_SCAN, Permission.REPORT_INCIDENT)),
    CYBER_OFFICER("Cybersecurity Officer", setOf(Permission.VIEW_SECURITY, Permission.VIEW_AUDIT, Permission.VIEW_ASSETS, Permission.REPORT_INCIDENT, Permission.TRIAGE_INCIDENT, Permission.REQUEST_SCAN, Permission.EXPORT_REPORT)),
    IT_SUPPORT("IT Support Officer", setOf(Permission.VIEW_INFRA, Permission.VIEW_ASSETS, Permission.REGISTER_ASSET, Permission.REPORT_INCIDENT)),
    INCIDENT_COMMANDER("Incident Commander", setOf(Permission.VIEW_SECURITY, Permission.VIEW_AUDIT, Permission.REPORT_INCIDENT, Permission.TRIAGE_INCIDENT, Permission.EXPORT_REPORT)),
    IT_MANAGER("IT Manager", setOf(Permission.VIEW_INFRA, Permission.VIEW_SECURITY, Permission.VIEW_ASSETS, Permission.APPROVE_CHANGE, Permission.EXPORT_REPORT)),
    MANAGEMENT("Read-only Management User", setOf(Permission.VIEW_INFRA, Permission.VIEW_SECURITY, Permission.VIEW_ASSETS))
}
data class Device(val id: String, val name: String, val kind: String, val address: String, val status: String, val detail: String)
data class Asset(val id: String, val hostname: String, val category: String, val ip: String, val owner: String, val location: String, val compliant: Boolean)
data class Incident(val id: String, val title: String, val severity: String, val category: String, val status: String, val description: String)
data class Finding(val id: String, val asset: String, val cve: String, val severity: String, val remediation: String)
data class Notice(val title: String, val time: String, val severity: String)

/** Demo-only in-memory repository. Never used as a source of production authorization or telemetry. */
interface OperationsRepository {
    val devices: StateFlow<List<Device>>
    val assets: StateFlow<List<Asset>>
    val incidents: StateFlow<List<Incident>>
    val findings: StateFlow<List<Finding>>
    val notices: StateFlow<List<Notice>>
    fun registerAsset(asset: Asset)
    fun reportIncident(incident: Incident)
}
class DemoRepository : OperationsRepository {
    override val devices = MutableStateFlow(listOf(
        Device("NET-001", "Core Router 01", "Router", "10.20.0.1", "Offline", "Upstream outage · office services may be affected"),
        Device("NET-002", "Broadcast Switch 02", "Switch", "10.20.2.10", "Online", "Latency 4 ms · 99.98% uptime"),
        Device("SRV-014", "Playout Server", "Server", "10.20.4.14", "Warning", "CPU 81% · RAM 68% · Disk 74%"),
        Device("SEC-003", "Perimeter Firewall", "Firewall", "10.20.0.254", "Online", "All interfaces operational"),
        Device("WIFI-08", "Newsroom AP", "Access point", "10.20.8.8", "Online", "32 clients · 12 ms latency")
    ))
    override val assets = MutableStateFlow(listOf(
        Asset("TBC-A-1042", "playout-01", "Server", "10.20.4.14", "Broadcast Engineering", "Mikocheni · Rack B3", true),
        Asset("TBC-A-1088", "newsroom-sw02", "Network switch", "10.20.2.10", "IT Networks", "Newsroom · Floor 2", true),
        Asset("TBC-A-1123", "studio-ws07", "Desktop", "10.20.6.27", "Unassigned", "Studio 3", false)
    ))
    override val incidents = MutableStateFlow(listOf(
        Incident("INC-2026-041", "Unusual sign-in attempts", "High", "Unauthorized access", "Investigating", "Multiple failed sign-ins on administrative account"),
        Incident("INC-2026-040", "Suspicious attachment reported", "Medium", "Phishing", "Open", "Email quarantined pending analysis")
    ))
    override val findings = MutableStateFlow(listOf(
        Finding("VUL-018", "playout-01", "CVE-2025-1234", "High", "Validate vendor advisory; patch in approved maintenance window"),
        Finding("VUL-019", "studio-ws07", "CVE-2024-5678", "Medium", "Update operating system and verify remediation")
    ))
    override val notices = MutableStateFlow(listOf(
        Notice("Core Router 01 is unreachable. Connected office network services may be affected.", "Demo · 09:42 EAT", "Critical"),
        Notice("Playout Server CPU crossed warning threshold", "Demo · 09:38 EAT", "Warning")
    ))
    override fun registerAsset(asset: Asset) { assets.value = listOf(asset) + assets.value }
    override fun reportIncident(incident: Incident) { incidents.value = listOf(incident) + incidents.value }
}

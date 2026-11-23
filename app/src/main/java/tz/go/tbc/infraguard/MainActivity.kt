package tz.go.tbc.infraguard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private val Navy = Color(0xFF102A43)
private val Blue = Color(0xFF1976D2)
private val Teal = Color(0xFF007F80)
private val Red = Color(0xFFC62828)
private val Amber = Color(0xFF996000)

class OperationsViewModel : ViewModel() {
    val repo: OperationsRepository = DemoRepository()
    var role by mutableStateOf<Role?>(Role.SYSTEM_ADMIN)
        private set
    var dark by mutableStateOf(false)
    fun login(selected: Role) { role = selected }
    var liveDevices by mutableStateOf<List<LiveDevice>>(emptyList())
        private set
    var monitorError by mutableStateOf<String?>(null)
        private set
    var monitorLoading by mutableStateOf(false)
        private set
    var monitorConnected by mutableStateOf(false)
        private set
    private var endpoint = ""
    private var accessToken = ""
    private val monitoringClient = MonitoringClient()
    fun connectMonitoring(url: String, token: String) {
        if (monitorLoading) return
        monitorLoading = true
        monitorError = null
        viewModelScope.launch {
            try {
                val readings = monitoringClient.fetch(url, token)
                endpoint = url
                accessToken = token
                liveDevices = readings
                monitorConnected = true
            } catch (e: Exception) {
                monitorError = e.message ?: "Unable to connect to monitoring service"
                disconnectMonitoring()
            } finally { monitorLoading = false }
        }
    }
    fun refreshMonitoring() { if (monitorConnected) connectMonitoring(endpoint, accessToken) }
    fun disconnectMonitoring() {
        endpoint = ""; accessToken = ""; liveDevices = emptyList(); monitorConnected = false
    }
    fun logout() { disconnectMonitoring() }
    fun allowed(permission: Permission) = role?.permissions?.contains(permission) == true
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent {
        val vm: OperationsViewModel = viewModel()
        val colors = if (vm.dark) darkColorScheme(primary = Color(0xFF9EC9FF), secondary = Color(0xFF76D6D4)) else lightColorScheme(primary = Navy, secondary = Blue, tertiary = Teal, error = Red)
        MaterialTheme(colorScheme = colors) {
            var showSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) { delay(1400); showSplash = false }
            when {
                showSplash -> SplashScreen()
                else -> AppShell(vm)
            }
        }
    } }
}
@Composable private fun BrandLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.tbc_nms_logo),
        contentDescription = "TBC Network and Security Management System shield emblem",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
@Composable private fun SplashScreen() {
    Surface(Modifier.fillMaxSize(), color = Navy) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            BrandLogo(Modifier.fillMaxWidth().heightIn(max = 360.dp))
            Spacer(Modifier.height(24.dp))
            Text("TBC InfraGuard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Infrastructure · Network · Security", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFC1D9EF))
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}
private data class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val tabs = listOf(Tab("Overview", Icons.Default.Dashboard), Tab("Infrastructure", Icons.Default.Router), Tab("Security", Icons.Default.Security), Tab("Assets", Icons.Default.Inventory2), Tab("More", Icons.Default.MoreHoriz))
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun AppShell(vm: OperationsViewModel) {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val current = backStackEntry?.destination?.route ?: "Overview"
    Scaffold(topBar = { TopAppBar(title = { Column { Text("TBC InfraGuard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(current.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium) } }, actions = {
        IconButton(onClick = { nav.navigate("notifications") }) { Icon(Icons.Default.Notifications, "Notifications") }
        IconButton(onClick = { nav.navigate("profile") }) { Icon(Icons.Default.AccountCircle, "Profile") }
    }) }, bottomBar = { NavigationBar { tabs.forEach { tab -> NavigationBarItem(selected = current == tab.label, onClick = { nav.navigate(tab.label) { popUpTo("Overview") { saveState = true }; launchSingleTop = true; restoreState = true } }, icon = { Icon(tab.icon, null) }, label = { Text(tab.label) }) } } }) { padding ->
        NavHost(navController = nav, startDestination = "Overview", modifier = Modifier.padding(padding)) {
            composable("Overview") { Overview(vm) }
            composable("Infrastructure") { Infrastructure(vm) }
            composable("Security") { Security(vm) }
            composable("Assets") { Assets(vm) }
            composable("More") { More(vm, onNavigate = { route -> nav.navigate(route) }) }
            composable("notifications") { Notifications(vm) }
            composable("profile") { Profile(vm) }
            composable("patches") { PendingModule("Patch management", "ONE → SOME → MANY rollout, approvals, rollback and emergency abort require a connected management service.") }
            composable("changes") { PendingModule("Change management", "Approval, maintenance checklists, dependency ordering and rollback require backend workflows and immutable audit events.") }
            composable("remote") { PendingModule("Remote access", "SSH and console sessions require an authorized gateway, step-up authentication and server-side auditing.") }
            composable("helpdesk") { PendingModule("IT helpdesk", "Ticket submission and technician assignment require an organizational ticketing service.") }
            composable("reports") { PendingModule("Reporting", "PDF and CSV exports require an authorized reporting endpoint with redaction and organizational timestamps.") }
        }
    }
}
@Composable private fun Page(title: String, subtitle: String, showDemo: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp)); if (showDemo) { DemoBanner(); Spacer(Modifier.height(18.dp)) }; content(); Spacer(Modifier.height(24.dp))
    }
}
@Composable private fun DemoBanner() { Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(12.dp)) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null); Spacer(Modifier.width(10.dp)); Text("DEMONSTRATION DATA · Not connected to TBC systems", style = MaterialTheme.typography.labelMedium) } } }
@Composable private fun Section(text: String) { Spacer(Modifier.height(16.dp)); Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(10.dp)) }
@Composable private fun Stat(label: String, value: String, color: Color = MaterialTheme.colorScheme.primary) { ElevatedCard(Modifier.width(150.dp).height(102.dp)) { Column(Modifier.padding(14.dp)) { Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color); Text(label, style = MaterialTheme.typography.labelMedium) } } }
@Composable private fun Status(text: String) { val color = when(text.lowercase()) { "offline", "critical", "high", "non-compliant" -> Red; "warning", "medium", "investigating" -> Amber; else -> Teal }; Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(16.dp)) { Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = color, style = MaterialTheme.typography.labelMedium) } }
@Composable private fun InfoCard(title: String, detail: String, badge: String? = null) { OutlinedCard(Modifier.fillMaxWidth().padding(bottom = 9.dp)) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); if (badge != null) Status(badge) }; Spacer(Modifier.height(5.dp)); Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun Overview(vm: OperationsViewModel) {
    val devices by vm.repo.devices.collectAsState(); val incidents by vm.repo.incidents.collectAsState(); val assets by vm.repo.assets.collectAsState()
    Page("Operations overview", "A single view across TBC technology operations") {
        Text("Snapshot · sample environment", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Stat("Monitored devices", "${devices.size}"); Stat("Online", "${devices.count { it.status == "Online" }}", Teal) }
        Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Stat("Open incidents", "${incidents.count { it.status != "Resolved" }}", Amber); Stat("Managed assets", "${assets.size}") }
        Section("Priority attention")
        InfoCard("Core Router 01 is unreachable", "Connected office network services may be affected. Downstream alerts should be suppressed by the monitoring backend.", "Critical")
        InfoCard("Playout Server resources elevated", "CPU 81% · RAM 68% · Disk 74%", "Warning")
        Section("Operational readiness")
        InfoCard("Wi-Fi discovery", "Open Infrastructure for real, on-device Wi-Fi information and manually initiated local discovery. Other dashboard numbers are sample data.", "Available")
    }
}
@Composable private fun Infrastructure(vm: OperationsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf(WifiDiscovery.current(context)) }
    var hosts by remember { mutableStateOf<List<DiscoveredHost>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var scanCompleted by remember { mutableStateOf(false) }
    var gatewayResult by remember { mutableStateOf<Boolean?>(null) }
    var checkedGateway by remember { mutableStateOf(false) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    DisposableEffect(context) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { snapshot = WifiDiscovery.current(context); hosts = emptyList(); scanCompleted = false; checkedGateway = false }
            override fun onLost(network: Network) { snapshot = WifiDiscovery.current(context); hosts = emptyList(); scanCompleted = false; checkedGateway = false; scanJob?.cancel() }
            override fun onCapabilitiesChanged(network: Network, capabilities: android.net.NetworkCapabilities) {
                snapshot = WifiDiscovery.current(context)
            }
            override fun onLinkPropertiesChanged(network: Network, properties: android.net.LinkProperties) {
                snapshot = WifiDiscovery.current(context)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { manager.unregisterNetworkCallback(callback); scanJob?.cancel() }
    }
    Page("Infrastructure", "Your current Wi-Fi connection", showDemo = false) {
        val wifi = snapshot
        if (wifi == null) {
            InfoCard("No active Wi-Fi connection", "Connect this phone to Wi-Fi to view its local address and gateway. Cellular connections are not scanned.")
        } else {
            Text("ON-DEVICE · CURRENT WI-FI", color = Teal, fontWeight = FontWeight.Bold)
            InfoCard("Connection", "Phone IP: ${wifi.ip.hostAddress} / ${wifi.prefix}\nGateway: ${wifi.gateway?.hostAddress ?: "Not provided by network"}")
            Text("Wi-Fi name is not shown because Android requires additional location permission for SSID access.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = {
                checkedGateway = false
                scope.launch { gatewayResult = WifiDiscovery.gatewayCheck(wifi); checkedGateway = true }
            }, enabled = wifi.gateway != null) { Text("Check gateway web ports") }
            if (checkedGateway) Text(if (gatewayResult == true) "Gateway TCP 80 or 443 responded" else "No response on TCP 80/443; gateway may still be reachable", style = MaterialTheme.typography.bodyMedium)
            Section("Local device discovery")
            Text("Only scan Wi-Fi networks you own or are authorized to inspect. A manual scan checks TCP ports 22, 80 and 443 on this private IPv4 subnet only (maximum 254 addresses). No data is uploaded.")
            val candidates = WifiDiscovery.candidates(wifi)
            if (candidates.isEmpty()) InfoCard("Scan unavailable", "Discovery requires a private IPv4 Wi-Fi subnet with prefix /24 through /30. Wi-Fi details remain available.")
            else {
                Row {
                    Button(onClick = {
                        hosts = emptyList(); scanCompleted = false; scanning = true
                        scanJob = scope.launch {
                            try { hosts = WifiDiscovery.discover(wifi); scanCompleted = true }
                            catch (_: CancellationException) { /* Network changed or user cancelled. */ }
                            finally { scanning = false }
                        }
                    }, enabled = !scanning) { Text(if (scanning) "Scanning…" else "Start discovery") }
                    if (scanning) { Spacer(Modifier.width(8.dp)); OutlinedButton(onClick = { scanJob?.cancel() }) { Text("Cancel") } }
                }
                if (scanCompleted && !scanning) {
                    Text("${hosts.size} hosts responded on selected ports · ${candidates.size} addresses eligible", style = MaterialTheme.typography.bodySmall)
                    hosts.forEach { InfoCard(it.address, "Open TCP ports: ${it.openPorts.joinToString()}", "Responded") }
                    Text("Not listed does not mean offline. Firewalls, client isolation and closed ports hide devices.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Section("Server monitoring (optional)")
        Text("The separate authenticated monitoring service remains available for approved remote targets; local discovery does not use it.", style = MaterialTheme.typography.bodySmall)
        MonitoringConnection(vm)
    }
}
@Composable private fun MonitoringConnection(vm: OperationsViewModel) {
    var expanded by remember { mutableStateOf(false) }
    var url by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide server monitoring" else "Show server monitoring") }
    if (!expanded) return
    if (!vm.monitorConnected) {
        OutlinedTextField(url, { url = it }, label = { Text("HTTPS monitoring origin") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(token, { token = it }, label = { Text("Access token") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.connectMonitoring(url, token); token = "" }, enabled = !vm.monitorLoading && url.isNotBlank() && token.isNotBlank()) { Text("Connect") }
    } else {
        Text("LIVE SERVER API · TCP checks")
        Row {
            Button(onClick = vm::refreshMonitoring, enabled = !vm.monitorLoading) { Text("Refresh") }
            OutlinedButton(onClick = vm::disconnectMonitoring) { Text("Disconnect") }
        }
        vm.liveDevices.forEach { InfoCard(it.name, "${it.address}:${it.port} · Checked ${it.checkedAt}", it.status) }
    }
    vm.monitorError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}
@Composable private fun Security(vm: OperationsViewModel) {
    val incidents by vm.repo.incidents.collectAsState(); val findings by vm.repo.findings.collectAsState(); var form by remember { mutableStateOf(false) }
    Page("Security operations", "Incidents and vulnerability governance") {
        if (!vm.allowed(Permission.VIEW_SECURITY)) { Restricted(); return@Page }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Stat("Open incidents", "${incidents.count { it.status == "Open" }}", Amber); Stat("Investigating", "${incidents.count { it.status == "Investigating" }}") }
        if (vm.allowed(Permission.REPORT_INCIDENT)) { Spacer(Modifier.height(14.dp)); Button(onClick = { form = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Report incident") } }
        Section("Incident queue"); incidents.forEach { InfoCard("${it.id} · ${it.title}", "${it.category} · ${it.status}\n${it.description}", it.severity) }
        Section("Vulnerability findings"); findings.forEach { InfoCard("${it.id} · ${it.asset}", "${it.cve} · ${it.remediation}", it.severity) }
        Section("Authorized actions"); InfoCard("Scan and inspection requests", "Unavailable in demonstration mode. Requires approved scope, endpoint agent, server-side permission checks and audit logging.")
        if (vm.allowed(Permission.VIEW_AUDIT)) InfoCard("Security audit logs", "Unavailable until a secured log API is connected. Demo does not contain sensitive logs.")
    }
    if (form) IncidentForm(onDismiss = { form = false }, onSave = { vm.repo.reportIncident(it); form = false })
}
@Composable private fun Assets(vm: OperationsViewModel) {
    val assets by vm.repo.assets.collectAsState(); var query by remember { mutableStateOf("") }; var form by remember { mutableStateOf(false) }
    Page("Asset inventory", "Ownership and compliance accountability") {
        if (!vm.allowed(Permission.VIEW_ASSETS)) { Restricted(); return@Page }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Stat("Compliant", "${assets.count { it.compliant }}", Teal); Stat("Needs review", "${assets.count { !it.compliant }}", Amber) }
        if (vm.allowed(Permission.REGISTER_ASSET)) { Spacer(Modifier.height(14.dp)); Button(onClick = { form = true }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Register demo asset") } }
        Section("Managed assets"); OutlinedTextField(query, { query = it }, label = { Text("Search ID, host or owner") }, leadingIcon = { Icon(Icons.Default.Search, null) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp)); assets.filter { it.id.contains(query, true) || it.hostname.contains(query, true) || it.owner.contains(query, true) }.forEach { InfoCard("${it.id} · ${it.hostname}", "${it.category} · ${it.ip}\n${it.owner} · ${it.location}", if (it.compliant) "Compliant" else "Non-compliant") }
        InfoCard("Host inspection", "Processes, ports, applications, updates and security settings require an authorized endpoint agent. Not available in demo mode.")
    }
    if (form) AssetForm(onDismiss = { form = false }, onSave = { vm.repo.registerAsset(it); form = false })
}
@Composable private fun Restricted() { InfoCard("Access restricted", "This role is not permitted to view this module. Production permissions must be enforced by the backend.") }
@Composable private fun More(vm: OperationsViewModel, onNavigate: (String) -> Unit) { Page("Workspace", "Tools and administration") {
    listOf("patches" to "Patch management", "changes" to "Changes & maintenance", "remote" to "Remote infrastructure", "helpdesk" to "IT helpdesk", "reports" to "Reports & exports").forEach { (route, label) -> OutlinedButton(onClick = { onNavigate(route) }, modifier = Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null) } }
    Section("Preferences"); Row(verticalAlignment = Alignment.CenterVertically) { Text("Dark mode", Modifier.weight(1f)); Switch(vm.dark, { vm.dark = it }) }
} }
@Composable private fun Notifications(vm: OperationsViewModel) { val notices by vm.repo.notices.collectAsState(); Page("Notifications", "Sample operational alerts") { notices.forEach { InfoCard(it.title, it.time, it.severity) }; InfoCard("Push notifications", "Unavailable until Firebase or an equivalent gateway is configured with server-side topic authorization.") } }
@Composable private fun Profile(vm: OperationsViewModel) { Page("Profile", "Local-only access") { InfoCard(vm.role?.title ?: "Unknown", "No organizational sign-in. Roles in sample modules do not enforce access."); Section("Permissions"); vm.role?.permissions?.forEach { Text("• ${it.name.replace('_', ' ')}", Modifier.padding(vertical = 4.dp)) }; Spacer(Modifier.height(20.dp)); OutlinedButton(onClick = vm::logout) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Clear server session") } } }
@Composable private fun PendingModule(title: String, message: String) { Page(title, "Integration required") { InfoCard("Not available in demonstration mode", message); Text("No operation was submitted or executed.", style = MaterialTheme.typography.bodySmall) } }
@Composable private fun IncidentForm(onDismiss: () -> Unit, onSave: (Incident) -> Unit) {
    var title by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var category by remember { mutableStateOf("Phishing") }; var severity by remember { mutableStateOf("Medium") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Report demo incident") }, text = { Column(Modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(title, { title = it }, label = { Text("Title *") }, singleLine = true)
        OutlinedTextField(description, { description = it }, label = { Text("Description *") }, minLines = 2)
        Text("Category"); Row(Modifier.horizontalScroll(rememberScrollState())) { listOf("Phishing", "Unauthorized access", "Policy violation", "Other").forEach { FilterChip(category == it, { category = it }, label = { Text(it) }, modifier = Modifier.padding(end = 5.dp)) } }
        Text("Severity"); Row(Modifier.horizontalScroll(rememberScrollState())) { listOf("Critical", "High", "Medium", "Low").forEach { FilterChip(severity == it, { severity = it }, label = { Text(it) }, modifier = Modifier.padding(end = 5.dp)) } }
    } }, confirmButton = { TextButton(enabled = title.isNotBlank() && description.isNotBlank(), onClick = { onSave(Incident("DEMO-${System.currentTimeMillis()}", title.trim(), severity, category, "Open", description.trim())) }) { Text("Save locally") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
@Composable private fun AssetForm(onDismiss: () -> Unit, onSave: (Asset) -> Unit) {
    var id by remember { mutableStateOf("") }; var hostname by remember { mutableStateOf("") }; var ip by remember { mutableStateOf("") }; var category by remember { mutableStateOf("") }; var owner by remember { mutableStateOf("") }; var location by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Register demo asset") }, text = { Column(Modifier.verticalScroll(rememberScrollState())) {
        listOf(Triple("Asset ID *", id) { v: String -> id = v }, Triple("Hostname *", hostname) { v: String -> hostname = v }, Triple("Category *", category) { v: String -> category = v }, Triple("IP address", ip) { v: String -> ip = v }, Triple("Owner *", owner) { v: String -> owner = v }, Triple("Location", location) { v: String -> location = v }).forEach { (label, value, update) -> OutlinedTextField(value, update, label = { Text(label) }, singleLine = true) }
    } }, confirmButton = { TextButton(enabled = id.isNotBlank() && hostname.isNotBlank() && category.isNotBlank() && owner.isNotBlank(), onClick = { onSave(Asset(id.trim(), hostname.trim(), category.trim(), ip.trim(), owner.trim(), location.trim(), false)) }) { Text("Save locally") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

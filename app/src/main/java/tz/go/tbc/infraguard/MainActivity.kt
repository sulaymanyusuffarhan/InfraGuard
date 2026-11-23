package tz.go.tbc.infraguard

import android.os.Bundle
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    var wifi by mutableStateOf<WifiSnapshot?>(null)
        private set
    var recorded by mutableStateOf<List<RecordedHost>>(emptyList())
        private set
    var detected by mutableStateOf<List<DiscoveredHost>>(emptyList())
        private set
    var scanning by mutableStateOf(false)
        private set
    var completedAt by mutableStateOf<Long?>(null)
        private set
    var discoveryError by mutableStateOf<String?>(null)
        private set
    private var lastNetworkKey: String? = null
    private var scanJob: Job? = null
    private var store: DiscoveryStore? = null

    fun refreshWifi(context: Context, force: Boolean = false) {
        val next = WifiDiscovery.current(context)
        val key = next?.let { "${it.network}:${it.ip.hostAddress}/${it.prefix}" }
        if (!force && key == lastNetworkKey) return
        scanJob?.cancel()
        lastNetworkKey = key
        wifi = next
        detected = emptyList()
        completedAt = null
        discoveryError = null
        scanning = false
        if (next == null) { recorded = emptyList(); return }
        val storage = store ?: DiscoveryStore(context.applicationContext).also { store = it }
        val subnet = WifiDiscovery.subnetKey(next)
        scanning = WifiDiscovery.candidates(next).isNotEmpty()
        scanJob = viewModelScope.launch {
            try {
                val previous = storage.list(subnet)
                if (lastNetworkKey != key) return@launch
                recorded = previous
                if (!scanning) return@launch
                val results = WifiDiscovery.discover(next)
                val now = System.currentTimeMillis()
                if (lastNetworkKey != key) return@launch
                storage.save(subnet, results, now)
                detected = results
                recorded = storage.list(subnet)
                completedAt = now
            } catch (_: CancellationException) {
                // Network changed or the user stopped discovery.
            } catch (e: Exception) {
                if (lastNetworkKey == key) discoveryError = e.message ?: "Discovery failed"
            } finally { if (lastNetworkKey == key) scanning = false }
        }
    }
    fun stopDiscovery() { scanJob?.cancel(); scanning = false; lastNetworkKey = null }
    fun clearRecords() {
        val current = wifi ?: return
        val storage = store ?: return
        scanJob?.cancel()
        scanning = false
        lastNetworkKey = null
        viewModelScope.launch { storage.clear(WifiDiscovery.subnetKey(current)); recorded = emptyList(); detected = emptyList(); completedAt = null }
    }
    var dark by mutableStateOf(false)
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(context, lifecycleOwner) {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) vm.refreshWifi(context) }
            override fun onLost(network: Network) { if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) vm.refreshWifi(context) }
            override fun onCapabilitiesChanged(network: Network, capabilities: android.net.NetworkCapabilities) {
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) vm.refreshWifi(context)
            }
            override fun onLinkPropertiesChanged(network: Network, properties: android.net.LinkProperties) {
                if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) vm.refreshWifi(context)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshWifi(context, force = true)
            if (event == Lifecycle.Event.ON_STOP) vm.stopDiscovery()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        manager.registerDefaultNetworkCallback(callback, Handler(Looper.getMainLooper()))
        vm.refreshWifi(context)
        onDispose { manager.unregisterNetworkCallback(callback); lifecycleOwner.lifecycle.removeObserver(observer); vm.stopDiscovery() }
    }
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
@Composable private fun Page(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp)); content(); Spacer(Modifier.height(24.dp))
    }
}
@Composable private fun Section(text: String) { Spacer(Modifier.height(16.dp)); Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(10.dp)) }
@Composable private fun Stat(label: String, value: String, color: Color = MaterialTheme.colorScheme.primary) { ElevatedCard(Modifier.width(150.dp).height(102.dp)) { Column(Modifier.padding(14.dp)) { Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color); Text(label, style = MaterialTheme.typography.labelMedium) } } }
@Composable private fun Status(text: String) { val color = when(text.lowercase()) { "offline", "critical", "high", "non-compliant" -> Red; "warning", "medium", "investigating" -> Amber; else -> Teal }; Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(16.dp)) { Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = color, style = MaterialTheme.typography.labelMedium) } }
@Composable private fun InfoCard(title: String, detail: String, badge: String? = null) { OutlinedCard(Modifier.fillMaxWidth().padding(bottom = 9.dp)) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); if (badge != null) Status(badge) }; Spacer(Modifier.height(5.dp)); Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun Overview(vm: OperationsViewModel) {
    Page("Operations overview", "Current phone network · local records") {
        val wifi = vm.wifi
        if (wifi == null) InfoCard("Wi-Fi disconnected", "Join Wi-Fi and open the app to start bounded device discovery.")
        else {
            InfoCard("Connected Wi-Fi", "Phone: ${wifi.ip.hostAddress} / ${wifi.prefix}\nGateway: ${wifi.gateway?.hostAddress ?: "Unavailable"}", "Connected")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Detected now", "${vm.detected.size}", Teal)
                Stat("Recorded", "${vm.recorded.size}")
            }
            Spacer(Modifier.height(10.dp))
            Text(if (vm.scanning) "Checking local devices…" else if (vm.completedAt != null) "Last check: ${formatTime(vm.completedAt!!)}" else "No completed check yet")
            InfoCard("Detection limit", "Only hosts answering TCP 22, 80 or 443 are detected. This cannot show the router's full Wi-Fi client list.")
        }
        Section("Other modules")
        InfoCard("No invented alerts or assets", "Security and inventory start empty. Network management, incidents and vulnerabilities need separate integrations.")
    }
}
private fun formatTime(time: Long): String = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(time))
@Composable private fun Infrastructure(vm: OperationsViewModel) {
    var gatewayResult by remember { mutableStateOf<Boolean?>(null) }
    var checkedGateway by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentContext = LocalContext.current
    Page("Infrastructure", "Local Wi-Fi discovery") {
        val wifi = vm.wifi
        if (wifi == null) InfoCard("No active Wi-Fi", "Connect to Wi-Fi, then return to the app. Cellular networks are not scanned.")
        else {
            InfoCard("Connected network", "Phone IP: ${wifi.ip.hostAddress} / ${wifi.prefix}\nGateway: ${wifi.gateway?.hostAddress ?: "Unavailable"}")
            Text("Automatic check runs when the app opens or joins Wi-Fi while foregrounded. Only scan networks you are permitted to inspect.", style = MaterialTheme.typography.bodySmall)
            val candidates = WifiDiscovery.candidates(wifi)
            if (candidates.isEmpty()) InfoCard("Discovery not supported", "Requires a private IPv4 subnet /24–/30. No targets were checked.")
            else {
                Row {
                    Button(onClick = { vm.refreshWifi(context = currentContext, force = true) }, enabled = !vm.scanning) { Text(if (vm.scanning) "Checking…" else "Check again") }
                    Spacer(Modifier.width(8.dp))
                    if (vm.scanning) OutlinedButton(onClick = vm::stopDiscovery) { Text("Stop") }
                }
                Text("${candidates.size} addresses eligible · TCP 22, 80, 443 · at most 20 probes concurrently", style = MaterialTheme.typography.bodySmall)
                vm.completedAt?.let { Text("Last completed ${formatTime(it)} · ${vm.detected.size} responded", style = MaterialTheme.typography.bodyMedium) }
                vm.discoveryError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Section("Responding this check")
                if (vm.detected.isEmpty()) Text("No responding hosts recorded in this check.")
                vm.detected.forEach { InfoCard(it.address, "Responsive TCP ports: ${it.openPorts.joinToString()}", "Responded") }
                Section("Previously recorded on ${WifiDiscovery.subnetKey(wifi)}")
                Text("Records are identified by subnet and IP, not Wi-Fi name; different networks can share a subnet. Old records are not proof a host is still online.", style = MaterialTheme.typography.bodySmall)
                vm.recorded.forEach { InfoCard(it.address, "Ports: ${it.ports} · Last seen: ${formatTime(it.lastSeen)}") }
                if (vm.recorded.isNotEmpty()) OutlinedButton(onClick = vm::clearRecords) { Text("Delete records for this subnet") }
            }
            OutlinedButton(onClick = {
                checkedGateway = false
                scope.launch { gatewayResult = WifiDiscovery.gatewayCheck(wifi); checkedGateway = true }
            }, enabled = wifi.gateway != null) { Text("Check gateway web ports") }
            if (checkedGateway) Text(if (gatewayResult == true) "Gateway answered TCP 80 or 443" else "No web-port response; gateway may still be reachable")
        }
        InfoCard("Coverage", "This cannot identify every device using Wi-Fi. Closed ports, client isolation and sleeping devices are invisible; wired hosts on the same subnet may appear.")
        Section("Optional server monitoring")
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
    Page("Security", "No security collector connected") {
        InfoCard("No security incidents available", "Wi-Fi discovery does not inspect traffic, identify vulnerabilities or generate security incidents. A trusted security service is needed for those functions.")
    }
}
@Composable private fun Assets(vm: OperationsViewModel) {
    Page("Assets", "Detected local addresses are not verified assets") {
        val wifi = vm.wifi
        if (wifi == null) InfoCard("No active Wi-Fi", "Join a network to see local discovery records.")
        else {
            Text("Recorded addresses on ${WifiDiscovery.subnetKey(wifi)}", style = MaterialTheme.typography.titleMedium)
            vm.recorded.forEach { InfoCard(it.address, "Responsive ports: ${it.ports} · Last seen ${formatTime(it.lastSeen)}") }
            if (vm.recorded.isEmpty()) InfoCard("No hosts recorded", "A bounded check starts when Wi-Fi connects while the app is open. Responding addresses appear here after it finishes.")
        }
        InfoCard("Not an asset inventory", "Discovery cannot determine owner, hostname, MAC address or whether a host uses Wi-Fi rather than Ethernet.")
    }
}
@Composable private fun More(vm: OperationsViewModel, onNavigate: (String) -> Unit) { Page("Workspace", "Tools and administration") {
    listOf("patches" to "Patch management", "changes" to "Changes & maintenance", "remote" to "Remote infrastructure", "helpdesk" to "IT helpdesk", "reports" to "Reports & exports").forEach { (route, label) -> OutlinedButton(onClick = { onNavigate(route) }, modifier = Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null) } }
    Section("Preferences"); Row(verticalAlignment = Alignment.CenterVertically) { Text("Dark mode", Modifier.weight(1f)); Switch(vm.dark, { vm.dark = it }) }
} }
@Composable private fun Notifications(vm: OperationsViewModel) {
    Page("Notifications", "No notification service connected") {
        InfoCard("No alerts", "Local TCP checks do not produce outage or security notifications. No alerts are generated.")
    }
}
@Composable private fun Profile(vm: OperationsViewModel) {
    Page("Local access", "No organization account required") {
        InfoCard("Private device records", "Discovered addresses are stored only in this app's local database and are not uploaded. Delete records from the Infrastructure screen.")
        if (vm.monitorConnected) OutlinedButton(onClick = vm::disconnectMonitoring) { Text("Disconnect optional server") }
    }
}
@Composable private fun PendingModule(title: String, message: String) { Page(title, "Integration required") { InfoCard("Integration unavailable", message); Text("No operation was submitted or executed.", style = MaterialTheme.typography.bodySmall) } }

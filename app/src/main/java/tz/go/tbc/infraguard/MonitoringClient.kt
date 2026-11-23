package tz.go.tbc.infraguard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class LiveDevice(val id: String, val name: String, val address: String, val port: Int,
                      val status: String, val latencyMs: Int?, val checkedAt: String)

/** HTTPS-only, read-only API client. Credentials are held in memory for this session only. */
class MonitoringClient {
    suspend fun fetch(baseUrl: String, token: String): List<LiveDevice> = withContext(Dispatchers.IO) {
        val base = URL(baseUrl.trim().trimEnd('/') + "/")
        require(base.protocol == "https" && base.host.isNotBlank() && base.userInfo == null && base.query == null && base.ref == null && base.path == "/") {
            "Enter an HTTPS origin only (for example https://monitor.example.org)"
        }
        require(token.isNotBlank()) { "Enter an access token" }
        val connection = (URL(base, "v1/monitoring/devices").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            connectTimeout = 5000
            readTimeout = 5000
            instanceFollowRedirects = false // never forward credentials to a redirect target
        }
        try {
            val code = connection.responseCode
            if (code != 200) throw IllegalStateException(when (code) {
                401, 403 -> "Access denied. Check your monitoring token."
                else -> "Monitoring service returned HTTP $code"
            })
            val raw = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONObject(raw).getJSONArray("devices")
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                LiveDevice(item.getString("id"), item.getString("name"), item.getString("address"),
                    item.getInt("port"), item.getString("status"),
                    if (item.isNull("latencyMs")) null else item.getInt("latencyMs"), item.getString("checkedAt"))
            }
        } finally {
            connection.disconnect()
        }
    }
}

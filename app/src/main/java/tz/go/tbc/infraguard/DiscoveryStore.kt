package tz.go.tbc.infraguard

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RecordedHost(val address: String, val ports: String, val lastSeen: Long)

/** App-private local records, keyed by subnet and IP. No network identifier is claimed. */
class DiscoveryStore(context: Context) : SQLiteOpenHelper(context.applicationContext, "wifi_hosts.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE hosts (subnet TEXT NOT NULL, address TEXT NOT NULL, ports TEXT NOT NULL, last_seen INTEGER NOT NULL, PRIMARY KEY(subnet, address))")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    suspend fun save(subnet: String, hosts: List<DiscoveredHost>, time: Long) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            hosts.forEach { host ->
                val values = ContentValues().apply {
                    put("subnet", subnet); put("address", host.address)
                    put("ports", host.openPorts.joinToString(", ")); put("last_seen", time)
                }
                db.insertWithOnConflict("hosts", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    suspend fun list(subnet: String): List<RecordedHost> = withContext(Dispatchers.IO) {
        readableDatabase.query("hosts", arrayOf("address", "ports", "last_seen"), "subnet = ?",
            arrayOf(subnet), null, null, "last_seen DESC", "500").use { cursor ->
            buildList { while (cursor.moveToNext()) add(RecordedHost(cursor.getString(0), cursor.getString(1), cursor.getLong(2))) }
        }
    }

    suspend fun clear(subnet: String) = withContext(Dispatchers.IO) {
        writableDatabase.delete("hosts", "subnet = ?", arrayOf(subnet))
    }
}

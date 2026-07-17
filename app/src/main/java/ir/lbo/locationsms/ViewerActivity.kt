package ir.lbo.locationsms

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main Viewer dashboard: shows received messages and parsed coordinates.
 * All configuration and command buttons live in ViewerCommandsActivity,
 * reached via the button at the bottom — keeping this screen focused on
 * just displaying incoming data.
 */
class ViewerActivity : LockProtectedActivity() {

    private lateinit var historyListView: ListView
    private lateinit var csvListView: ListView
    private lateinit var debugLastSmsText: TextView
    private lateinit var historyAdapter: ViewerHistoryAdapter
    private lateinit var csvAdapter: ArrayAdapter<String>

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private val refreshListener: () -> Unit = { runOnUiThread { refreshAll() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        historyListView = findViewById(R.id.historyListView)
        csvListView = findViewById(R.id.csvListView)
        debugLastSmsText = findViewById(R.id.debugLastSmsText)

        historyAdapter = ViewerHistoryAdapter(this, ViewerHistoryStore.getAll(this))
        historyListView.adapter = historyAdapter
        historyListView.emptyView = findViewById<TextView>(R.id.emptyHistoryText)

        csvAdapter = ArrayAdapter(this, R.layout.item_csv_row, ArrayList())
        csvListView.adapter = csvAdapter
        csvListView.emptyView = findViewById<TextView>(R.id.emptyCsvText)

        findViewById<Button>(R.id.openCommandsButton).setOnClickListener {
            startActivity(Intent(this, ViewerCommandsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
        NewMessageNotifier.addListener(refreshListener)
    }

    override fun onPause() {
        super.onPause()
        NewMessageNotifier.removeListener(refreshListener)
    }

    private fun refreshAll() {
        val entries = ViewerHistoryStore.getAll(this)
        historyAdapter.updateItems(entries)

        val csvLines = entries
            .filter { it.latitude != null && it.longitude != null }
            .sortedByDescending { it.timestamp }
            .map { entry ->
                "${dateFormat.format(Date(entry.timestamp))},${entry.latitude},${entry.longitude}"
            }
        csvAdapter.clear()
        csvAdapter.addAll(csvLines)

        val settings = SettingsRepository(this)
        debugLastSmsText.text = settings.getLastRawSmsDebug() ?: "—"
    }
}

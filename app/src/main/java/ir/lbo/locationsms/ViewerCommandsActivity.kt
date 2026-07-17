package ir.lbo.locationsms

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class ViewerCommandsActivity : LockProtectedActivity() {

    private lateinit var settings: SettingsRepository
    private lateinit var trackerPhoneInput: EditText
    private lateinit var autosendIntervalInput: EditText

    private val requiredPermissions: Array<String> by lazy {
        val list = mutableListOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                Toast.makeText(this, "مجوزها با موفقیت داده شد", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "بدون مجوز پیامک، این حالت نمی‌تواند دستور بفرستد یا پاسخ دریافت کند",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer_commands)

        settings = SettingsRepository(this)
        trackerPhoneInput = findViewById(R.id.trackerPhoneInput)
        autosendIntervalInput = findViewById(R.id.autosendIntervalInput)

        trackerPhoneInput.setText(settings.getTrackerViewerPhone() ?: "")
        autosendIntervalInput.setText("30")

        findViewById<Button>(R.id.saveTrackerPhoneButton).setOnClickListener { onSaveTrackerPhone() }
        findViewById<Button>(R.id.sendlocButton).setOnClickListener { sendCommand("sendloc") }
        findViewById<Button>(R.id.pingButton).setOnClickListener { sendCommand("ping") }
        findViewById<Button>(R.id.autosendOnButton).setOnClickListener { onAutosendOnClicked() }
        findViewById<Button>(R.id.autosendOffButton).setOnClickListener { sendCommand("autosend off") }
        findViewById<Button>(R.id.sendlogButton).setOnClickListener { sendCommand("sendlog") }
        findViewById<Button>(R.id.dellogButton).setOnClickListener { sendCommand("dellog") }
        findViewById<Button>(R.id.clearHistoryButton).setOnClickListener { onClearHistoryClicked() }
        findViewById<Button>(R.id.showMapButton).setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        if (!hasAllPermissions()) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun onSaveTrackerPhone() {
        val phone = trackerPhoneInput.text.toString().trim()
        if (phone.isEmpty()) {
            Toast.makeText(this, "شماره گوشی ردیاب را وارد کنید", Toast.LENGTH_SHORT).show()
            return
        }
        settings.saveTrackerViewerPhone(phone)
        Toast.makeText(this, "شماره ذخیره شد", Toast.LENGTH_SHORT).show()

        if (!hasAllPermissions()) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun onAutosendOnClicked() {
        val minutesText = autosendIntervalInput.text.toString().trim()
        val minutes = minutesText.toLongOrNull() ?: 30L
        val safeMinutes = if (minutes < 15) 15L else minutes
        autosendIntervalInput.setText(safeMinutes.toString())
        sendCommand("autosend on $safeMinutes")
    }

    private fun onClearHistoryClicked() {
        ViewerHistoryStore.clear(this)
        Toast.makeText(this, "تاریخچه پاک شد", Toast.LENGTH_SHORT).show()
    }

    private fun sendCommand(command: String) {
        val phone = settings.getTrackerViewerPhone()
        if (phone.isNullOrBlank()) {
            Toast.makeText(this, "ابتدا شماره گوشی ردیاب را ذخیره کنید", Toast.LENGTH_SHORT).show()
            return
        }
        if (!hasAllPermissions()) {
            Toast.makeText(this, "ابتدا مجوز پیامک را بدهید", Toast.LENGTH_SHORT).show()
            permissionLauncher.launch(requiredPermissions)
            return
        }

        CommandSender.send(this, phone, command)
        Toast.makeText(this, "دستور «$command» ارسال شد", Toast.LENGTH_SHORT).show()
    }

    private fun hasAllPermissions(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
}

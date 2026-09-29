package app.voltune.panel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.voltune.panel.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var streamRows: StreamRows
    private var waitingForOverlay = false

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            startPanel()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding()

        streamRows = StreamRows(this, layoutInflater, binding.streamList) { openDndSettings() }
        streamRows.build()

        binding.panelSwitch.setOnClickListener {
            if (binding.panelSwitch.isChecked) enablePanel() else stopPanel()
        }
    }

    override fun onStart() {
        super.onStart()
        streamRows.startWatching()
        binding.panelSwitch.isChecked = PanelService.isRunning
    }

    override fun onResume() {
        super.onResume()
        if (waitingForOverlay) {
            waitingForOverlay = false
            if (Settings.canDrawOverlays(this)) askNotificationsThenStart()
        }
    }

    override fun onStop() {
        streamRows.stopWatching()
        super.onStop()
    }

    private fun enablePanel() {
        if (Settings.canDrawOverlays(this)) {
            askNotificationsThenStart()
            return
        }

        binding.panelSwitch.isChecked = false
        waitingForOverlay = true
        Toast.makeText(this, R.string.overlay_permission_hint, Toast.LENGTH_LONG).show()
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        )
    }

    private fun askNotificationsThenStart() {
        val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

        if (needsAsk) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startPanel()
        }
    }

    private fun startPanel() {
        ContextCompat.startForegroundService(this, Intent(this, PanelService::class.java))
        binding.panelSwitch.isChecked = true
    }

    private fun stopPanel() {
        stopService(Intent(this, PanelService::class.java))
    }

    private fun openDndSettings() {
        Toast.makeText(this, R.string.dnd_access_hint, Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun applySystemBarPadding() {
        val base = binding.root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                base + bars.left,
                base + bars.top,
                base + bars.right,
                base + bars.bottom
            )
            insets
        }
    }
}

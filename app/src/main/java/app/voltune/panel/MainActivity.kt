package app.voltune.panel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.voltune.panel.databinding.ActivityMainBinding
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var streamRows: StreamRows
    private lateinit var prefs: Prefs
    private val accentDots = mutableListOf<View>()
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

        prefs = Prefs(this)

        streamRows = StreamRows(this, layoutInflater, binding.streamList) { openDndSettings() }
        streamRows.build()
        streamRows.applyAccent(prefs.accentColor)

        binding.panelSwitch.setOnClickListener {
            if (binding.panelSwitch.isChecked) enablePanel() else stopPanel()
        }

        setupTriggerSide()
        setupAccentDots()
        setupOpacity()
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

    private fun setupTriggerSide() {
        binding.sideGroup.check(if (prefs.triggerOnLeft) R.id.sideLeft else R.id.sideRight)
        binding.sideGroup.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            prefs.triggerOnLeft = id == R.id.sideLeft
            refreshPanel()
        }
    }

    private fun setupAccentDots() {
        Prefs.ACCENTS.forEachIndexed { index, _ ->
            val dot = View(this).apply {
                contentDescription = getString(R.string.cd_accent_option, index + 1)
                setOnClickListener { selectAccent(index) }
            }
            val size = dp(36)
            val params = LinearLayout.LayoutParams(size, size).apply { marginEnd = dp(14) }
            binding.accentRow.addView(dot, params)
            accentDots += dot
        }
        paintAccentDots()
    }

    private fun selectAccent(index: Int) {
        prefs.accentIndex = index
        paintAccentDots()
        streamRows.applyAccent(prefs.accentColor)
        refreshPanel()
    }

    private fun paintAccentDots() {
        accentDots.forEachIndexed { index, dot ->
            dot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Prefs.ACCENTS[index])
                if (index == prefs.accentIndex) setStroke(dp(3), Color.WHITE)
            }
        }
    }

    private fun setupOpacity() {
        val slider = binding.opacitySlider
        slider.value = prefs.panelOpacity.coerceIn(40, 100).toFloat()
        showOpacity(slider.value.toInt())

        slider.addOnChangeListener { _, value, _ -> showOpacity(value.toInt()) }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) = Unit

            override fun onStopTrackingTouch(slider: Slider) {
                prefs.panelOpacity = slider.value.toInt()
                refreshPanel()
            }
        })
    }

    private fun showOpacity(value: Int) {
        binding.opacityValue.text = getString(R.string.percent_format, value)
    }

    private fun refreshPanel() {
        if (!PanelService.isRunning) return
        startService(Intent(this, PanelService::class.java).setAction(PanelService.ACTION_REFRESH))
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

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

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

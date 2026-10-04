package app.voltune.panel

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.voltune.panel.databinding.ActivityMainBinding
import app.voltune.panel.databinding.ItemSectionBinding

class MainActivity : AppCompatActivity() {

    private enum class Section(@StringRes val title: Int, @StringRes val desc: Int) {
        PRESETS(R.string.section_presets, R.string.section_presets_desc),
        THEME(R.string.section_theme, R.string.section_theme_desc),
        SLIDERS(R.string.section_sliders, R.string.section_sliders_desc),
        TRIGGER(R.string.section_trigger, R.string.section_trigger_desc),
        SHORTCUTS(R.string.section_shortcuts, R.string.section_shortcuts_desc),
        BEHAVIOUR(R.string.section_behaviour, R.string.section_behaviour_desc),
        PROFILES(R.string.section_profiles, R.string.section_profiles_desc),
        AUDIO(R.string.section_audio, R.string.section_audio_desc),
        WIDGETS(R.string.section_widgets, R.string.section_widgets_desc),
        BACKUP(R.string.section_backup, R.string.section_backup_desc),
        PERMISSIONS(R.string.section_permissions, R.string.section_permissions_desc)
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var store: ConfigStore
    private var configHandle: SharedPreferences.OnSharedPreferenceChangeListener? = null
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

        store = ConfigStore(this)
        binding.preview.panel.onDndAccessNeeded = { openDndSettings() }

        buildSections()

        binding.panelSwitch.setOnClickListener {
            if (binding.panelSwitch.isChecked) enablePanel() else stopPanel()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.panelSwitch.isChecked = PanelService.isRunning
        configHandle = store.observe { binding.preview.show(store.load()) }
        binding.preview.show(store.load())
    }

    override fun onResume() {
        super.onResume()
        if (waitingForOverlay) {
            waitingForOverlay = false
            if (Settings.canDrawOverlays(this)) askNotificationsThenStart()
        }
    }

    override fun onStop() {
        configHandle?.let { store.stopObserving(it) }
        configHandle = null
        super.onStop()
    }

    private fun buildSections() {
        val gap = dp(6)
        Section.entries.forEach { section ->
            val card = ItemSectionBinding.inflate(layoutInflater, binding.sectionGrid, false)
            card.sectionTitle.setText(section.title)
            card.sectionDesc.setText(section.desc)
            card.root.setOnClickListener { openSection(section) }

            val params = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply {
                width = 0
                setMargins(gap, gap, gap, gap)
            }
            binding.sectionGrid.addView(card.root, params)
        }
    }

    private fun openSection(section: Section) {
        val screen = when (section) {
            Section.PRESETS -> PresetsActivity::class.java
            Section.THEME -> ThemeStudioActivity::class.java
            else -> null
        }

        if (screen == null) {
            Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(this, screen))
        }
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

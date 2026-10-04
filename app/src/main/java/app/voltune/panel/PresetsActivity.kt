package app.voltune.panel

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.voltune.panel.Presets.Part
import app.voltune.panel.databinding.ActivityPresetsBinding
import app.voltune.panel.databinding.ItemPresetBinding
import app.voltune.panel.databinding.ItemSettingSliderBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider

class PresetsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPresetsBinding
    private lateinit var store: ConfigStore
    private val cards = mutableListOf<Pair<Presets.Preset, ItemPresetBinding>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPresetsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding()

        store = ConfigStore(this)
        val current = store.load()
        setupSize(current)
        buildGrid(binding.luxuryGrid, Presets.luxury, current)
        buildGrid(binding.signatureGrid, Presets.signature, current)
        buildGrid(binding.classicGrid, Presets.classic, current)
    }

    override fun onResume() {
        super.onResume()
        highlight(store.load())
    }

    private fun setupSize(current: PanelConfig) {
        val row = ItemSettingSliderBinding.inflate(layoutInflater, binding.sizeContainer, true)
        val slider = row.settingSlider
        val start = 70 + (current.scale.coerceIn(70, 150) - 70) / 5 * 5

        row.settingLabel.setText(R.string.panel_size)
        row.settingValue.text = getString(R.string.percent_format, start)
        slider.valueFrom = 70f
        slider.valueTo = 150f
        slider.stepSize = 5f
        slider.value = start.toFloat()

        slider.addOnChangeListener { _, value, _ ->
            row.settingValue.text = getString(R.string.percent_format, value.toInt())
        }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) = Unit

            override fun onStopTrackingTouch(slider: Slider) {
                store.update { it.copy(scale = slider.value.toInt()) }
            }
        })
    }

    private fun buildGrid(grid: GridLayout, presets: List<Presets.Preset>, current: PanelConfig) {
        val gap = dp(6)

        presets.forEach { preset ->
            val card = ItemPresetBinding.inflate(layoutInflater, grid, false)
            card.presetName.setText(preset.name)
            card.presetBadge.visibility = if (preset.premium) View.VISIBLE else View.GONE
            card.presetPreview.interactive = false
            card.presetPreview.show(Presets.apply(current, preset, Part.ALL))
            card.root.setOnClickListener { askHowToApply(preset) }

            val params = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply {
                width = 0
                setMargins(gap, gap, gap, gap)
            }
            grid.addView(card.root, params)
            cards += preset to card
        }
    }

    private fun askHowToApply(preset: Presets.Preset) {
        val options = arrayOf(
            getString(R.string.apply_all),
            getString(R.string.apply_layout),
            getString(R.string.apply_colors),
            getString(R.string.apply_customize)
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(preset.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> applyPreset(preset, Part.ALL)
                    1 -> applyPreset(preset, Part.LAYOUT)
                    2 -> applyPreset(preset, Part.COLORS)
                    else -> {
                        applyPreset(preset, Part.ALL, announce = false)
                        startActivity(Intent(this, ThemeStudioActivity::class.java))
                    }
                }
            }
            .show()
    }

    private fun applyPreset(preset: Presets.Preset, part: Part, announce: Boolean = true) {
        val updated = Presets.apply(store.load(), preset, part)
        store.save(updated)
        highlight(updated)

        if (announce) {
            val message = getString(R.string.preset_applied, getString(preset.name))
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun highlight(current: PanelConfig) {
        cards.forEach { (preset, card) ->
            card.root.strokeWidth = if (Presets.isActive(current, preset)) dp(2) else 0
        }
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

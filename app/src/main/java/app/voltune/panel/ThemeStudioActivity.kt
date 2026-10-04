package app.voltune.panel

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.voltune.panel.databinding.ActivityThemeStudioBinding
import app.voltune.panel.databinding.ItemColorRowBinding
import app.voltune.panel.databinding.ItemSettingSliderBinding
import app.voltune.panel.databinding.ItemSettingSwitchBinding
import com.google.android.material.slider.Slider
import java.util.Locale

class ThemeStudioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeStudioBinding
    private lateinit var store: ConfigStore
    private var config = PanelConfig()

    private val palette = intArrayOf(
        0xFF0F1115.toInt(), 0xFF14161C.toInt(), 0xFF1E2230.toInt(), 0xFF2A2E3A.toInt(),
        0xFF3A3F4F.toInt(), 0xFF9AA5B1.toInt(), 0xFFE6E8EE.toInt(), 0xFFFFFFFF.toInt(),
        0xFFF4EFE6.toInt(), 0xFF6C5CE7.toInt(), 0xFF4DA3FF.toInt(), 0xFF00D1B2.toInt(),
        0xFF7BC67E.toInt(), 0xFFFFB84D.toInt(), 0xFFFF6B81.toInt(), 0xFFB07D62.toInt()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityThemeStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding()

        store = ConfigStore(this)
        config = store.load()

        binding.preview.panel.onDndAccessNeeded = { openDndSettings() }
        binding.preview.show(config)

        setupLayout()
        setupColors()
        setupShape()
        setupSize()
        setupDisplay()
    }

    private fun update(persist: Boolean = true, change: (PanelConfig) -> PanelConfig) {
        config = change(config)
        binding.preview.show(config)
        if (persist) store.save(config)
    }

    private fun setupLayout() {
        val selected = if (config.layout == PanelConfig.Layout.COLUMNS) R.id.layoutColumns else R.id.layoutRows
        binding.layoutGroup.check(selected)
        binding.layoutGroup.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            val layout = if (id == R.id.layoutColumns) {
                PanelConfig.Layout.COLUMNS
            } else {
                PanelConfig.Layout.ROWS
            }
            update { it.copy(layout = layout) }
        }
    }

    private fun setupColors() {
        addColorRow(R.string.color_panel, { it.colors.panel }) { c, color ->
            c.copy(colors = c.colors.copy(panel = color))
        }
        addColorRow(R.string.color_track, { it.colors.track }) { c, color ->
            c.copy(colors = c.colors.copy(track = color))
        }
        addColorRow(R.string.color_fill, { it.colors.fill }) { c, color ->
            c.copy(colors = c.colors.copy(fill = color))
        }
        addColorRow(R.string.color_icon, { it.colors.icon }) { c, color ->
            c.copy(colors = c.colors.copy(icon = color))
        }
        addColorRow(R.string.color_text, { it.colors.text }) { c, color ->
            c.copy(colors = c.colors.copy(text = color))
        }
    }

    private fun setupShape() {
        val box = binding.shapeContainer
        addSlider(box, R.string.panel_corners, 0..40, 1, config.panelCorner, ::dpText) { c, v ->
            c.copy(panelCorner = v)
        }
        addSlider(box, R.string.bar_corners, 0..40, 1, config.barCorner, ::dpText) { c, v ->
            c.copy(barCorner = v)
        }
    }

    private fun setupSize() {
        val box = binding.sizeContainer
        addSlider(box, R.string.bar_thickness, 24..72, 2, config.barThickness, ::dpText) { c, v ->
            c.copy(barThickness = v)
        }
        addSlider(box, R.string.bar_length, 120..280, 10, config.barLength, ::dpText) { c, v ->
            c.copy(barLength = v)
        }
        addSlider(box, R.string.bar_spacing, 0..24, 1, config.barSpacing, ::dpText) { c, v ->
            c.copy(barSpacing = v)
        }
    }

    private fun setupDisplay() {
        val box = binding.displayContainer
        addSwitch(box, R.string.show_icons, config.showIcons) { c, on -> c.copy(showIcons = on) }
        addSwitch(box, R.string.show_level, config.showLevel) { c, on -> c.copy(showLevel = on) }
        addSlider(box, R.string.panel_opacity, 40..100, 5, config.opacity, ::percentText) { c, v ->
            c.copy(opacity = v)
        }
    }

    private fun addColorRow(
        @StringRes label: Int,
        read: (PanelConfig) -> Int,
        write: (PanelConfig, Int) -> PanelConfig
    ) {
        val row = ItemColorRowBinding.inflate(layoutInflater, binding.colorsContainer, false)
        row.colorLabel.setText(label)

        val swatches = mutableListOf<Pair<Int, View>>()

        fun paint() {
            val current = read(config)
            swatches.forEach { (color, view) -> view.background = swatch(color, color == current) }
        }

        palette.forEach { color ->
            val view = View(this).apply {
                contentDescription = getString(R.string.cd_color_option, hex(color))
                setOnClickListener {
                    update { write(it, color) }
                    paint()
                }
            }
            val size = dp(34)
            val params = LinearLayout.LayoutParams(size, size).apply { marginEnd = dp(10) }
            row.colorSwatches.addView(view, params)
            swatches += color to view
        }

        paint()
        binding.colorsContainer.addView(row.root)
    }

    private fun addSlider(
        container: LinearLayout,
        @StringRes label: Int,
        range: IntRange,
        step: Int,
        value: Int,
        format: (Int) -> String,
        write: (PanelConfig, Int) -> PanelConfig
    ) {
        val row = ItemSettingSliderBinding.inflate(layoutInflater, container, false)
        val slider = row.settingSlider
        val start = snap(value, range, step)

        row.settingLabel.setText(label)
        row.settingValue.text = format(start)
        slider.valueFrom = range.first.toFloat()
        slider.valueTo = range.last.toFloat()
        slider.stepSize = step.toFloat()
        slider.value = start.toFloat()

        slider.addOnChangeListener { _, v, fromUser ->
            row.settingValue.text = format(v.toInt())
            if (fromUser) update(persist = false) { write(it, v.toInt()) }
        }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) = Unit

            override fun onStopTrackingTouch(slider: Slider) {
                update { write(it, slider.value.toInt()) }
            }
        })

        container.addView(row.root)
    }

    private fun addSwitch(
        container: LinearLayout,
        @StringRes label: Int,
        checked: Boolean,
        write: (PanelConfig, Boolean) -> PanelConfig
    ) {
        val row = ItemSettingSwitchBinding.inflate(layoutInflater, container, false)
        row.settingLabel.setText(label)
        row.settingSwitch.isChecked = checked
        row.settingSwitch.setOnCheckedChangeListener { _, on -> update { write(it, on) } }
        container.addView(row.root)
    }

    private fun swatch(color: Int, selected: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        if (selected) {
            val ring = if (ColorUtils.calculateLuminance(color) > 0.6) {
                ContextCompat.getColor(this@ThemeStudioActivity, R.color.vt_primary)
            } else {
                0xFFFFFFFF.toInt()
            }
            setStroke(dp(3), ring)
        } else {
            setStroke(dp(1), ContextCompat.getColor(this@ThemeStudioActivity, R.color.vt_surface_high))
        }
    }

    private fun snap(value: Int, range: IntRange, step: Int): Int {
        val clamped = value.coerceIn(range)
        return range.first + (clamped - range.first) / step * step
    }

    private fun dpText(value: Int) = getString(R.string.dp_format, value)

    private fun percentText(value: Int) = getString(R.string.percent_format, value)

    private fun hex(color: Int) = "#%06X".format(Locale.ROOT, color and 0xFFFFFF)

    private fun openDndSettings() {
        Toast.makeText(this, R.string.dnd_access_hint, Toast.LENGTH_LONG).show()
        startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun applySystemBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }
}

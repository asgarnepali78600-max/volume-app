package app.voltune.panel

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.voltune.panel.PanelConfig.Animation
import app.voltune.panel.PanelConfig.BarStyle
import app.voltune.panel.PanelConfig.Layout
import app.voltune.panel.PanelConfig.Placement
import app.voltune.panel.PanelConfig.Position
import app.voltune.panel.databinding.ActivityThemeStudioBinding
import app.voltune.panel.databinding.ItemColorRowBinding
import app.voltune.panel.databinding.ItemGroupBinding
import app.voltune.panel.databinding.ItemSettingSliderBinding
import app.voltune.panel.databinding.ItemSettingSwitchBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout
import java.util.Locale

class ThemeStudioActivity : AppCompatActivity() {

    private enum class Tab(@StringRes val title: Int) {
        STYLE(R.string.tab_style),
        COLORS(R.string.tab_colors),
        SHAPE(R.string.tab_shape),
        PANEL(R.string.tab_panel),
        TEXT(R.string.tab_text),
        MOTION(R.string.tab_motion)
    }

    private lateinit var binding: ActivityThemeStudioBinding
    private lateinit var store: ConfigStore
    private lateinit var initial: PanelConfig
    private var config = PanelConfig()
    private val history = ArrayDeque<PanelConfig>()
    private var currentTab = Tab.STYLE

    private val palette = longArrayOf(
        0xFF0F1115, 0xFF14161C, 0xFF1E2230, 0xFF2A2E3A, 0xFF3A3F4F,
        0xFF0B1622, 0xFF1A1210, 0xFF2B0A14, 0xFF9AA5B1, 0xFFE6E8EE,
        0xFFFFFFFF, 0xFFF4EFE6, 0x33FFFFFF, 0x66FFFFFF, 0xFF6C5CE7,
        0xFF9D8CFF, 0xFF4DA3FF, 0xFF00D1B2, 0xFF7BC67E, 0xFFFFB84D,
        0xFFFF6B81, 0xFFB07D62, 0xFFD4AF37, 0xFFF7E7A1, 0xFFB76E79
    ).map { it.toInt() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityThemeStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding()

        store = ConfigStore(this)
        config = store.load()
        initial = config

        binding.previewCard.clipToOutline = true
        binding.preview.panel.onDndAccessNeeded = { openDndSettings() }
        binding.preview.show(config)

        binding.undoButton.setOnClickListener { undo() }
        binding.resetButton.setOnClickListener { reset() }
        binding.playButton.setOnClickListener { play() }

        setupBackdrop()
        setupTabs()
        refreshUndo()
    }

    private fun commit(change: (PanelConfig) -> PanelConfig) {
        val updated = change(config)
        if (updated == config) return
        history.addLast(config)
        if (history.size > MAX_HISTORY) history.removeFirst()
        config = updated
        binding.preview.show(config)
        store.save(config)
        refreshUndo()
    }

    private fun previewOnly(change: (PanelConfig) -> PanelConfig) {
        config = change(config)
        binding.preview.show(config)
    }

    private fun undo() {
        val previous = history.removeLastOrNull() ?: return
        config = previous
        binding.preview.show(config)
        store.save(config)
        refreshUndo()
        renderTab()
    }

    private fun reset() {
        if (config == initial) return
        commit { initial }
        renderTab()
        Toast.makeText(this, R.string.reset_done, Toast.LENGTH_SHORT).show()
    }

    private fun refreshUndo() {
        binding.undoButton.isEnabled = history.isNotEmpty()
    }

    private fun play() {
        val panel = binding.preview.panel
        panel.animate().cancel()
        panel.translationX = 0f
        panel.scaleX = 1f
        panel.scaleY = 1f
        binding.preview.show(config)
        PanelAnimator.enter(panel, config.animation, config.trigger.onLeft)
    }

    private fun setupBackdrop() {
        binding.backdropGroup.check(R.id.backdropDark)
        applyBackdrop(R.id.backdropDark)
        binding.backdropGroup.addOnButtonCheckedListener { _, id, checked ->
            if (checked) applyBackdrop(id)
        }
    }

    private fun applyBackdrop(id: Int) {
        binding.backdrop.background = when (id) {
            R.id.backdropLight -> ColorDrawable(0xFFF2F3F7.toInt())
            R.id.backdropClear -> checkerDrawable()
            else -> ColorDrawable(0xFF0B0C10.toInt())
        }
    }

    private fun checkerDrawable(): Drawable {
        val cell = dp(10)
        val bitmap = Bitmap.createBitmap(cell * 2, cell * 2, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.color = 0xFFE9EBF0.toInt()
        canvas.drawRect(0f, 0f, cell * 2f, cell * 2f, paint)
        paint.color = 0xFFD3D7DF.toInt()
        canvas.drawRect(0f, 0f, cell.toFloat(), cell.toFloat(), paint)
        canvas.drawRect(cell.toFloat(), cell.toFloat(), cell * 2f, cell * 2f, paint)
        return BitmapDrawable(resources, bitmap).apply {
            setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
    }

    private fun setupTabs() {
        Tab.entries.forEach { binding.tabs.addTab(binding.tabs.newTab().setText(it.title)) }
        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = Tab.entries[tab.position]
                renderTab()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        renderTab()
    }

    private fun renderTab() {
        binding.tabContent.removeAllViews()
        when (currentTab) {
            Tab.STYLE -> buildStyleTab()
            Tab.COLORS -> buildColorsTab()
            Tab.SHAPE -> buildShapeTab()
            Tab.PANEL -> buildPanelTab()
            Tab.TEXT -> buildTextTab()
            Tab.MOTION -> buildMotionTab()
        }
        binding.tabScroll.scrollTo(0, 0)
    }

    private fun buildStyleTab() {
        val layoutBox = group(R.string.theme_layout)
        addChoices(
            layoutBox, null,
            listOf(Layout.COLUMNS to R.string.layout_columns, Layout.ROWS to R.string.layout_rows),
            config.layout
        ) { c, v -> c.copy(layout = v) }

        val styleBox = group(R.string.theme_style)
        addChoices(
            styleBox, null,
            listOf(
                BarStyle.SOLID to R.string.style_solid,
                BarStyle.GRADIENT to R.string.style_gradient,
                BarStyle.GLASS to R.string.style_glass,
                BarStyle.SEGMENTED to R.string.style_segmented,
                BarStyle.LINE to R.string.style_line
            ),
            config.barStyle
        ) { c, v -> c.copy(barStyle = v) }

        val outlineBox = group(R.string.bar_outline)
        addSlider(outlineBox, R.string.bar_outline, 0..4, 1, config.barOutline, ::dpText) { c, v ->
            c.copy(barOutline = v)
        }
        hint(outlineBox, R.string.hint_outline)
    }

    private fun buildColorsTab() {
        val box = group(R.string.theme_colors)
        hint(box, R.string.hint_colors)

        addColorRow(box, R.string.color_panel, { it.colors.panel }) { c, color ->
            val linked = c.colors.panelEnd == c.colors.panel
            c.copy(colors = c.colors.copy(panel = color, panelEnd = if (linked) color else c.colors.panelEnd))
        }
        addColorRow(box, R.string.color_panel_end, { it.colors.panelEnd }) { c, color ->
            c.copy(colors = c.colors.copy(panelEnd = color))
        }
        addColorRow(box, R.string.color_track, { it.colors.track }) { c, color ->
            c.copy(colors = c.colors.copy(track = color))
        }
        addColorRow(box, R.string.color_fill, { it.colors.fill }) { c, color ->
            val linked = c.colors.fillEnd == c.colors.fill
            c.copy(colors = c.colors.copy(fill = color, fillEnd = if (linked) color else c.colors.fillEnd))
        }
        addColorRow(box, R.string.color_fill_end, { it.colors.fillEnd }) { c, color ->
            c.copy(colors = c.colors.copy(fillEnd = color))
        }
        addColorRow(box, R.string.color_icon, { it.colors.icon }) { c, color ->
            c.copy(colors = c.colors.copy(icon = color))
        }
        addColorRow(box, R.string.color_text, { it.colors.text }) { c, color ->
            c.copy(colors = c.colors.copy(text = color))
        }
        addColorRow(box, R.string.color_border, { it.colors.border }) { c, color ->
            c.copy(colors = c.colors.copy(border = color))
        }
        addColorRow(box, R.string.color_outline, { it.colors.outline }) { c, color ->
            c.copy(colors = c.colors.copy(outline = color))
        }
    }

    private fun buildShapeTab() {
        val panelSize = group(R.string.panel_size)
        addSlider(panelSize, R.string.panel_size, 70..150, 5, config.scale, ::percentText) { c, v ->
            c.copy(scale = v)
        }
        hint(panelSize, R.string.hint_panel_size)

        val shape = group(R.string.theme_shape)
        addSlider(shape, R.string.panel_corners, 0..40, 1, config.panelCorner, ::dpText) { c, v ->
            c.copy(panelCorner = v)
        }
        addSlider(shape, R.string.bar_corners, 0..40, 1, config.barCorner, ::dpText) { c, v ->
            c.copy(barCorner = v)
        }
        addSlider(shape, R.string.border_width, 0..4, 1, config.borderWidth, ::dpText) { c, v ->
            c.copy(borderWidth = v)
        }

        val size = group(R.string.theme_size)
        addSlider(size, R.string.bar_thickness, 24..72, 2, config.barThickness, ::dpText) { c, v ->
            c.copy(barThickness = v)
        }
        addSlider(size, R.string.bar_length, 120..280, 10, config.barLength, ::dpText) { c, v ->
            c.copy(barLength = v)
        }
        addSlider(size, R.string.bar_spacing, 0..24, 1, config.barSpacing, ::dpText) { c, v ->
            c.copy(barSpacing = v)
        }
        addSlider(size, R.string.panel_opacity, 40..100, 5, config.opacity, ::percentText) { c, v ->
            c.copy(opacity = v)
        }
    }

    private fun buildPanelTab() {
        val box = group(R.string.theme_panel)
        addSwitch(box, R.string.panel_background, config.panelBackground) { c, on ->
            c.copy(panelBackground = on)
        }
        hint(box, R.string.hint_panel_background)
        addChoices(
            box, R.string.panel_position,
            listOf(
                Position.TOP to R.string.position_top,
                Position.CENTER to R.string.position_center,
                Position.BOTTOM to R.string.position_bottom
            ),
            config.position
        ) { c, v -> c.copy(position = v) }
    }

    private fun buildTextTab() {
        val places = listOf(
            Placement.INSIDE to R.string.place_inside,
            Placement.START to R.string.place_start,
            Placement.END to R.string.place_end
        )

        val icons = group(R.string.theme_icons)
        addSwitch(icons, R.string.show_icons, config.showIcons) { c, on -> c.copy(showIcons = on) }
        addChoices(icons, R.string.icon_position, places, config.iconPosition) { c, v ->
            c.copy(iconPosition = v)
        }

        val level = group(R.string.theme_level)
        addSwitch(level, R.string.show_level, config.showLevel) { c, on -> c.copy(showLevel = on) }
        addChoices(level, R.string.level_position, places, config.levelPosition) { c, v ->
            c.copy(levelPosition = v)
        }
        hint(level, R.string.hint_place)

        val names = group(R.string.theme_names)
        addSwitch(names, R.string.show_labels, config.showLabels) { c, on -> c.copy(showLabels = on) }
    }

    private fun buildMotionTab() {
        val box = group(R.string.theme_animation)
        addChoices(
            box, null,
            listOf(
                Animation.NONE to R.string.anim_none,
                Animation.FADE to R.string.anim_fade,
                Animation.SLIDE to R.string.anim_slide,
                Animation.POP to R.string.anim_pop
            ),
            config.animation,
            after = { play() }
        ) { c, v -> c.copy(animation = v) }
        hint(box, R.string.hint_play)
    }

    private fun group(@StringRes title: Int): LinearLayout {
        val group = ItemGroupBinding.inflate(layoutInflater, binding.tabContent, false)
        group.groupTitle.setText(title)
        binding.tabContent.addView(group.root)
        return group.groupContent
    }

    private fun hint(container: LinearLayout, @StringRes text: Int) {
        val view = TextView(this).apply {
            setText(text)
            setTextColor(ContextCompat.getColor(context, R.color.vt_text_muted))
            textSize = 12f
            setPadding(0, dp(6), 0, dp(4))
        }
        container.addView(view)
    }

    private fun label(container: LinearLayout, @StringRes text: Int) {
        val view = TextView(this).apply {
            setText(text)
            setTextColor(ContextCompat.getColor(context, R.color.vt_text))
            textSize = 15f
            setPadding(0, dp(10), 0, 0)
        }
        container.addView(view)
    }

    private fun <T> addChoices(
        container: LinearLayout,
        @StringRes title: Int?,
        options: List<Pair<T, Int>>,
        selected: T,
        after: () -> Unit = {},
        write: (PanelConfig, T) -> PanelConfig
    ) {
        if (title != null) label(container, title)

        val chips = ChipGroup(this).apply {
            isSingleSelection = true
            isSelectionRequired = true
        }
        options.forEach { (value, text) ->
            val chip = layoutInflater.inflate(R.layout.item_choice_chip, chips, false) as Chip
            chip.id = View.generateViewId()
            chip.setText(text)
            chip.isChecked = value == selected
            chip.setOnCheckedChangeListener { _, checked ->
                if (!checked) return@setOnCheckedChangeListener
                commit { write(it, value) }
                after()
            }
            chips.addView(chip)
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(4)
            bottomMargin = dp(4)
        }
        container.addView(chips, params)
    }

    private fun addColorRow(
        container: LinearLayout,
        @StringRes label: Int,
        read: (PanelConfig) -> Int,
        write: (PanelConfig, Int) -> PanelConfig
    ) {
        val row = ItemColorRowBinding.inflate(layoutInflater, container, false)
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
                    commit { write(it, color) }
                    paint()
                }
            }
            val size = dp(34)
            val params = LinearLayout.LayoutParams(size, size).apply { marginEnd = dp(10) }
            row.colorSwatches.addView(view, params)
            swatches += color to view
        }

        paint()
        container.addView(row.root)
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
        var dragStart: PanelConfig? = null

        row.settingLabel.setText(label)
        row.settingValue.text = format(start)
        slider.valueFrom = range.first.toFloat()
        slider.valueTo = range.last.toFloat()
        slider.stepSize = step.toFloat()
        slider.value = start.toFloat()

        slider.addOnChangeListener { _, v, fromUser ->
            row.settingValue.text = format(v.toInt())
            if (fromUser) previewOnly { write(it, v.toInt()) }
        }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                dragStart = config
            }

            override fun onStopTrackingTouch(slider: Slider) {
                val final = write(config, slider.value.toInt())
                dragStart?.let { config = it }
                dragStart = null
                commit { final }
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
        row.settingSwitch.setOnCheckedChangeListener { _, on -> commit { write(it, on) } }
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

    private fun hex(color: Int) = "#%08X".format(Locale.ROOT, color)

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

    private companion object {
        const val MAX_HISTORY = 40
    }
}

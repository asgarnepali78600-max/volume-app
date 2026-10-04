package app.voltune.panel

import android.app.NotificationManager
import android.content.Context
import android.content.res.ColorStateList
import android.database.ContentObserver
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import app.voltune.panel.PanelConfig.BarStyle
import app.voltune.panel.PanelConfig.Placement
import kotlin.math.max
import kotlin.math.min

class PanelView(context: Context) : LinearLayout(context) {

    var onDndAccessNeeded: () -> Unit = {}

    private class Bound(val stream: Stream, val bar: VolumeBar, val levelView: TextView?) {
        var dragging = false
        var requested = 0
        var blocked = false
    }

    private val audio = context.getSystemService(AudioManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val bound = mutableListOf<Bound>()
    private var watching = false
    private var scale = 1f

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            bound.filterNot { it.dragging }.forEach { sync(it) }
        }
    }

    init {
        clipChildren = false
        clipToPadding = false
    }

    fun apply(config: PanelConfig) {
        removeAllViews()
        bound.clear()
        scale = config.scale / 100f

        val columns = config.layout == PanelConfig.Layout.COLUMNS
        val colors = config.colors
        orientation = if (columns) HORIZONTAL else VERTICAL
        gravity = if (columns) Gravity.CENTER_VERTICAL else Gravity.START
        alpha = config.opacity / 100f

        background = if (config.panelBackground) {
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(colors.panel, colors.panelEnd)
            ).apply {
                cornerRadius = dp(config.panelCorner).toFloat()
                if (config.borderWidth > 0) setStroke(dp(config.borderWidth), colors.border)
            }
        } else {
            null
        }
        val pad = dp(14)
        setPadding(pad, pad, pad, pad)

        config.enabledStreams.forEachIndexed { index, stream ->
            val slot = buildSlot(config, stream, columns)
            val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            if (index > 0) {
                if (columns) params.marginStart = dp(config.barSpacing)
                else params.topMargin = dp(config.barSpacing)
            }
            addView(slot, params)
        }
    }

    private fun buildSlot(config: PanelConfig, stream: Stream, columns: Boolean): View {
        val colors = config.colors
        val iconPlace = if (config.showIcons) config.iconPosition else null
        val levelPlace = if (config.showLevel) config.levelPosition else null

        val bar = VolumeBar(context).apply {
            scale = this@PanelView.scale
            style = config.barStyle
            vertical = columns
            cornerRadius = dp(config.barCorner).toFloat()
            outlineWidth = dp(config.barOutline).toFloat()
            showLevel = levelPlace == Placement.INSIDE
            icon = if (iconPlace == Placement.INSIDE) {
                ContextCompat.getDrawable(context, stream.icon)
            } else {
                null
            }
            contentDescription = context.getString(stream.label)
            setColors(
                colors.track, colors.fill, colors.fillEnd,
                colors.icon, colors.text, colors.outline
            )

            if (!config.panelBackground && style in FLOATING_STYLES) {
                elevation = dp(6).toFloat()
                outlineProvider = roundedOutline(cornerRadius + outlineWidth)
            }
        }

        val levelView = if (levelPlace == Placement.START || levelPlace == Placement.END) {
            smallText(colors.text, bold = true)
        } else {
            null
        }

        val slot = LinearLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }

        if (columns) {
            buildColumnSlot(slot, config, stream, bar, levelView, iconPlace, levelPlace)
        } else {
            buildRowSlot(slot, config, stream, bar, levelView, iconPlace, levelPlace)
        }

        bind(stream, bar, levelView)
        return slot
    }

    private fun buildColumnSlot(
        slot: LinearLayout,
        config: PanelConfig,
        stream: Stream,
        bar: VolumeBar,
        levelView: TextView?,
        iconPlace: Placement?,
        levelPlace: Placement?
    ) {
        val textColor = config.colors.text
        val minWidth = when {
            config.showLabels -> 56
            levelView != null -> 40
            else -> 0
        }
        val slotWidth = dp(max(config.barThickness, minWidth))
        slot.orientation = VERTICAL
        slot.gravity = Gravity.CENTER_HORIZONTAL

        fun addLevel(top: Boolean) {
            val view = levelView ?: return
            view.gravity = Gravity.CENTER
            slot.addView(view, LayoutParams(slotWidth, LayoutParams.WRAP_CONTENT).apply {
                if (top) bottomMargin = dp(6) else topMargin = dp(6)
            })
        }

        fun addIcon(top: Boolean) {
            slot.addView(iconView(stream, textColor), LayoutParams(dp(22), dp(22)).apply {
                if (top) bottomMargin = dp(8) else topMargin = dp(8)
            })
        }

        if (levelPlace == Placement.START) addLevel(top = true)
        if (iconPlace == Placement.START) addIcon(top = true)
        slot.addView(bar, LayoutParams(dp(config.barThickness), dp(config.barLength)))
        if (iconPlace == Placement.END) addIcon(top = false)
        if (levelPlace == Placement.END) addLevel(top = false)
        if (config.showLabels) {
            slot.addView(
                labelView(stream, textColor, Gravity.CENTER),
                LayoutParams(slotWidth, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) }
            )
        }
    }

    private fun buildRowSlot(
        slot: LinearLayout,
        config: PanelConfig,
        stream: Stream,
        bar: VolumeBar,
        levelView: TextView?,
        iconPlace: Placement?,
        levelPlace: Placement?
    ) {
        val textColor = config.colors.text
        slot.orientation = HORIZONTAL
        slot.gravity = Gravity.CENTER_VERTICAL

        fun addLevel(left: Boolean) {
            val view = levelView ?: return
            view.gravity = if (left) Gravity.START else Gravity.END
            slot.addView(view, LayoutParams(dp(44), LayoutParams.WRAP_CONTENT).apply {
                if (left) marginEnd = dp(8) else marginStart = dp(8)
            })
        }

        fun addIcon(left: Boolean) {
            slot.addView(iconView(stream, textColor), LayoutParams(dp(22), dp(22)).apply {
                if (left) marginEnd = dp(10) else marginStart = dp(10)
            })
        }

        if (config.showLabels) {
            slot.addView(
                labelView(stream, textColor, Gravity.START),
                LayoutParams(dp(84), LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(8) }
            )
        }
        if (iconPlace == Placement.START) addIcon(left = true)
        if (levelPlace == Placement.START) addLevel(left = true)
        slot.addView(bar, LayoutParams(dp(config.barLength), dp(config.barThickness)))
        if (levelPlace == Placement.END) addLevel(left = false)
        if (iconPlace == Placement.END) addIcon(left = false)
    }

    private fun iconView(stream: Stream, color: Int) = ImageView(context).apply {
        setImageDrawable(ContextCompat.getDrawable(context, stream.icon))
        imageTintList = ColorStateList.valueOf(color)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun labelView(stream: Stream, color: Int, align: Int) =
        smallText(color, bold = false).apply {
            setText(stream.label)
            gravity = align
            alpha = 0.85f
        }

    private fun smallText(color: Int, bold: Boolean) = TextView(context).apply {
        setTextColor(color)
        textSize = 11f * scale
        if (bold) typeface = Typeface.DEFAULT_BOLD
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    private fun roundedOutline(radius: Float) = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            val r = min(radius, min(view.width, view.height) / 2f)
            outline.setRoundRect(0, 0, view.width, view.height, r)
        }
    }

    private fun bind(stream: Stream, bar: VolumeBar, levelView: TextView?) {
        val item = Bound(stream, bar, levelView)
        bar.setRange(minVolume(stream.audioType), audio.getStreamMaxVolume(stream.audioType))

        bar.onTouchStart = {
            item.dragging = true
            item.blocked = false
        }
        bar.onUserChange = { level ->
            setVolume(item, level)
            showLevel(item, level)
        }
        bar.onTouchEnd = {
            item.dragging = false
            sync(item)
            checkRefused(item)
        }

        bound += item
        sync(item)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == View.VISIBLE) startWatching() else stopWatching()
    }

    override fun onDetachedFromWindow() {
        stopWatching()
        super.onDetachedFromWindow()
    }

    private fun startWatching() {
        if (watching) return
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        watching = true
        bound.forEach { sync(it) }
    }

    private fun stopWatching() {
        if (!watching) return
        context.contentResolver.unregisterContentObserver(observer)
        watching = false
    }

    private fun setVolume(item: Bound, level: Int) {
        item.requested = level
        try {
            if (level > 0 && item.stream.followsRinger &&
                audio.ringerMode != AudioManager.RINGER_MODE_NORMAL
            ) {
                audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
            audio.setStreamVolume(item.stream.audioType, level, 0)
        } catch (e: SecurityException) {
            item.blocked = true
        }
    }

    private fun checkRefused(item: Bound) {
        if (!item.stream.followsRinger) return
        val current = audio.getStreamVolume(item.stream.audioType)
        val refused = item.blocked || (item.requested > 0 && current == 0)
        if (refused && !notifications.isNotificationPolicyAccessGranted) {
            onDndAccessNeeded()
        }
        item.blocked = false
    }

    private fun sync(item: Bound) {
        val current = audio.getStreamVolume(item.stream.audioType)
        item.bar.setLevel(current)
        showLevel(item, item.bar.level)
    }

    private fun showLevel(item: Bound, level: Int) {
        val percent = level * 100 / item.bar.max
        val text = context.getString(R.string.percent_format, percent)
        item.bar.levelText = text
        item.levelView?.text = text
    }

    private fun minVolume(type: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(type) else 0

    private fun dp(value: Int) = (value * resources.displayMetrics.density * scale).toInt()

    private companion object {
        val FLOATING_STYLES = setOf(BarStyle.SOLID, BarStyle.GRADIENT, BarStyle.GLASS)
    }
}

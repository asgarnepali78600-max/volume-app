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

        val columns = config.layout == PanelConfig.Layout.COLUMNS
        val colors = config.colors
        orientation = if (columns) HORIZONTAL else VERTICAL
        gravity = if (columns) Gravity.BOTTOM else Gravity.START
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
        val iconOutside = config.showIcons && config.iconPosition == Placement.OUTSIDE
        val levelOutside = config.showLevel && config.levelPosition == Placement.OUTSIDE

        val bar = VolumeBar(context).apply {
            style = config.barStyle
            vertical = columns
            cornerRadius = dp(config.barCorner).toFloat()
            showLevel = config.showLevel && config.levelPosition == Placement.INSIDE
            icon = if (config.showIcons && config.iconPosition == Placement.INSIDE) {
                ContextCompat.getDrawable(context, stream.icon)
            } else {
                null
            }
            contentDescription = context.getString(stream.label)
            setColors(colors.track, colors.fill, colors.fillEnd, colors.icon, colors.text)

            if (!config.panelBackground && style in FLOATING_STYLES) {
                elevation = dp(6).toFloat()
                outlineProvider = roundedOutline(cornerRadius)
            }
        }

        val levelView = if (levelOutside) smallText(colors.text, bold = true) else null
        val slot = LinearLayout(context).apply {
            clipChildren = false
            clipToPadding = false
        }

        if (columns) {
            val minWidth = when {
                config.showLabels -> 56
                levelOutside -> 40
                else -> 0
            }
            val slotWidth = dp(max(config.barThickness, minWidth))
            slot.orientation = VERTICAL
            slot.gravity = Gravity.CENTER_HORIZONTAL

            levelView?.let {
                it.gravity = Gravity.CENTER
                slot.addView(it, LayoutParams(slotWidth, LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(6)
                })
            }
            slot.addView(bar, LayoutParams(dp(config.barThickness), dp(config.barLength)))
            if (iconOutside) {
                slot.addView(iconView(stream, colors.text), LayoutParams(dp(22), dp(22)).apply {
                    topMargin = dp(8)
                })
            }
            if (config.showLabels) {
                slot.addView(
                    labelView(stream, colors.text, Gravity.CENTER),
                    LayoutParams(slotWidth, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(6) }
                )
            }
        } else {
            slot.orientation = HORIZONTAL
            slot.gravity = Gravity.CENTER_VERTICAL

            if (config.showLabels) {
                slot.addView(
                    labelView(stream, colors.text, Gravity.START),
                    LayoutParams(dp(84), LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(8) }
                )
            }
            if (iconOutside) {
                slot.addView(iconView(stream, colors.text), LayoutParams(dp(22), dp(22)).apply {
                    marginEnd = dp(10)
                })
            }
            slot.addView(bar, LayoutParams(dp(config.barLength), dp(config.barThickness)))
            levelView?.let {
                it.gravity = Gravity.END
                slot.addView(it, LayoutParams(dp(44), LayoutParams.WRAP_CONTENT).apply {
                    marginStart = dp(8)
                })
            }
        }

        bind(stream, bar, levelView)
        return slot
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
        textSize = 11f
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

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        val FLOATING_STYLES = setOf(BarStyle.SOLID, BarStyle.GRADIENT, BarStyle.GLASS)
    }
}

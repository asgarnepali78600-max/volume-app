package app.voltune.panel

import android.app.NotificationManager
import android.content.Context
import android.database.ContentObserver
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat

class PanelView(context: Context) : LinearLayout(context) {

    var onDndAccessNeeded: () -> Unit = {}

    private class Bound(val stream: Stream, val bar: VolumeBar) {
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

    fun apply(config: PanelConfig) {
        removeAllViews()
        bound.clear()

        val columns = config.layout == PanelConfig.Layout.COLUMNS
        val colors = config.colors
        orientation = if (columns) HORIZONTAL else VERTICAL
        alpha = config.opacity / 100f

        background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(colors.panel, colors.panelEnd)
        ).apply {
            cornerRadius = dp(config.panelCorner).toFloat()
            if (config.borderWidth > 0) setStroke(dp(config.borderWidth), colors.border)
        }
        val pad = dp(14)
        setPadding(pad, pad, pad, pad)

        config.enabledStreams.forEachIndexed { index, stream ->
            val bar = VolumeBar(context).apply {
                style = config.barStyle
                vertical = columns
                cornerRadius = dp(config.barCorner).toFloat()
                showLevel = config.showLevel
                icon = if (config.showIcons) ContextCompat.getDrawable(context, stream.icon) else null
                contentDescription = context.getString(stream.label)
                setColors(colors.track, colors.fill, colors.fillEnd, colors.icon, colors.text)
            }

            val params = if (columns) {
                LayoutParams(dp(config.barThickness), dp(config.barLength))
            } else {
                LayoutParams(dp(config.barLength), dp(config.barThickness))
            }
            if (index > 0) {
                if (columns) params.marginStart = dp(config.barSpacing)
                else params.topMargin = dp(config.barSpacing)
            }

            addView(bar, params)
            bind(stream, bar)
        }
    }

    private fun bind(stream: Stream, bar: VolumeBar) {
        val item = Bound(stream, bar)
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
        item.bar.levelText = context.getString(R.string.percent_format, percent)
    }

    private fun minVolume(type: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(type) else 0

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}

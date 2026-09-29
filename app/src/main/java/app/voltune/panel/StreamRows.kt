package app.voltune.panel

import android.app.NotificationManager
import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ViewGroup
import app.voltune.panel.databinding.ItemStreamBinding
import com.google.android.material.slider.Slider

class StreamRows(
    private val context: Context,
    private val inflater: LayoutInflater,
    private val container: ViewGroup,
    private val onDndAccessNeeded: () -> Unit = {}
) {

    private class Row(val type: Int, val view: ItemStreamBinding) {
        var min = 0
        var max = 1
        var dragging = false
        var requested = 0
        var blocked = false
    }

    private val streams = listOf(
        AudioManager.STREAM_MUSIC to R.string.stream_media,
        AudioManager.STREAM_RING to R.string.stream_ring,
        AudioManager.STREAM_NOTIFICATION to R.string.stream_notification,
        AudioManager.STREAM_ALARM to R.string.stream_alarm,
        AudioManager.STREAM_VOICE_CALL to R.string.stream_call,
        AudioManager.STREAM_SYSTEM to R.string.stream_system
    )

    private val ringerLinked = setOf(
        AudioManager.STREAM_RING,
        AudioManager.STREAM_NOTIFICATION,
        AudioManager.STREAM_SYSTEM
    )

    private val audio = context.getSystemService(AudioManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val rows = mutableListOf<Row>()

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            syncAll()
        }
    }

    fun build() {
        for ((type, label) in streams) {
            val item = ItemStreamBinding.inflate(inflater, container, false)
            val row = Row(type, item)

            row.min = minVolume(type)
            row.max = audio.getStreamMaxVolume(type)
            if (row.max <= row.min) {
                row.max = row.min + 1
                item.streamSlider.isEnabled = false
            }

            item.streamName.setText(label)
            item.streamSlider.valueFrom = row.min.toFloat()
            item.streamSlider.valueTo = row.max.toFloat()
            item.streamSlider.value = row.min.toFloat()

            item.streamSlider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) setVolume(row, value.toInt())
                showPercent(row, value.toInt())
            }

            item.streamSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: Slider) {
                    row.dragging = true
                    row.blocked = false
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    row.dragging = false
                    sync(row)
                    checkRefused(row)
                }
            })

            container.addView(item.root)
            rows += row
        }
    }

    fun startWatching() {
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        syncAll()
    }

    fun stopWatching() {
        context.contentResolver.unregisterContentObserver(observer)
    }

    private fun syncAll() {
        rows.filterNot { it.dragging }.forEach { sync(it) }
    }

    private fun setVolume(row: Row, level: Int) {
        row.requested = level
        try {
            if (level > 0 && row.type in ringerLinked &&
                audio.ringerMode != AudioManager.RINGER_MODE_NORMAL
            ) {
                audio.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
            audio.setStreamVolume(row.type, level, 0)
        } catch (e: SecurityException) {
            row.blocked = true
            if (!row.dragging) sync(row)
        }
    }

    private fun checkRefused(row: Row) {
        if (row.type !in ringerLinked) return
        val refused = row.blocked || (row.requested > 0 && audio.getStreamVolume(row.type) == 0)
        if (refused && !notifications.isNotificationPolicyAccessGranted) {
            onDndAccessNeeded()
        }
        row.blocked = false
    }

    private fun sync(row: Row) {
        val current = audio.getStreamVolume(row.type).coerceIn(row.min, row.max)
        row.view.streamSlider.value = current.toFloat()
        showPercent(row, current)
    }

    private fun showPercent(row: Row, level: Int) {
        val percent = level * 100 / row.max
        row.view.streamValue.text = context.getString(R.string.percent_format, percent)
    }

    private fun minVolume(type: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(type) else 0
}

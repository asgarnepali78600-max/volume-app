package app.voltune.panel

import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.voltune.panel.databinding.ActivityMainBinding
import app.voltune.panel.databinding.ItemStreamBinding
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private class StreamRow(val type: Int, val view: ItemStreamBinding) {
        var min = 0
        var max = 1
        var dragging = false
    }

    private val streams = listOf(
        AudioManager.STREAM_MUSIC to R.string.stream_media,
        AudioManager.STREAM_RING to R.string.stream_ring,
        AudioManager.STREAM_NOTIFICATION to R.string.stream_notification,
        AudioManager.STREAM_ALARM to R.string.stream_alarm,
        AudioManager.STREAM_VOICE_CALL to R.string.stream_call,
        AudioManager.STREAM_SYSTEM to R.string.stream_system
    )

    private lateinit var binding: ActivityMainBinding
    private lateinit var audio: AudioManager
    private val rows = mutableListOf<StreamRow>()

    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            rows.filterNot { it.dragging }.forEach { sync(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding()

        audio = getSystemService(AudioManager::class.java)
        buildStreamRows()
    }

    override fun onStart() {
        super.onStart()
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver)
        rows.forEach { sync(it) }
    }

    override fun onStop() {
        contentResolver.unregisterContentObserver(volumeObserver)
        super.onStop()
    }

    private fun buildStreamRows() {
        for ((type, label) in streams) {
            val item = ItemStreamBinding.inflate(layoutInflater, binding.streamList, false)
            val row = StreamRow(type, item)

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
                }

                override fun onStopTrackingTouch(slider: Slider) {
                    row.dragging = false
                    sync(row)
                }
            })

            binding.streamList.addView(item.root)
            rows += row
        }
    }

    private fun setVolume(row: StreamRow, level: Int) {
        try {
            audio.setStreamVolume(row.type, level, 0)
        } catch (e: SecurityException) {
            if (!row.dragging) sync(row)
        }
    }

    private fun sync(row: StreamRow) {
        val current = audio.getStreamVolume(row.type).coerceIn(row.min, row.max)
        row.view.streamSlider.value = current.toFloat()
        showPercent(row, current)
    }

    private fun showPercent(row: StreamRow, level: Int) {
        val percent = level * 100 / row.max
        row.view.streamValue.text = getString(R.string.percent_format, percent)
    }

    private fun minVolume(type: Int): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(type) else 0

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

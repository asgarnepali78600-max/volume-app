package app.voltune.panel

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.voltune.panel.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var audio: AudioManager

    private val mediaStream = AudioManager.STREAM_MUSIC
    private var mediaMax = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding()

        audio = getSystemService(AudioManager::class.java)
        setupMediaSlider()
    }

    override fun onResume() {
        super.onResume()
        syncMediaSlider()
    }

    private fun setupMediaSlider() {
        mediaMax = audio.getStreamMaxVolume(mediaStream)
        binding.mediaSlider.valueTo = mediaMax.toFloat()

        binding.mediaSlider.addOnChangeListener { _, value, fromUser ->
            val level = value.toInt()
            if (fromUser) {
                audio.setStreamVolume(mediaStream, level, 0)
            }
            showPercent(level)
        }
    }

    private fun syncMediaSlider() {
        val current = audio.getStreamVolume(mediaStream)
        binding.mediaSlider.value = current.toFloat()
        showPercent(current)
    }

    private fun showPercent(level: Int) {
        val percent = level * 100 / mediaMax
        binding.mediaValue.text = getString(R.string.percent_format, percent)
    }

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

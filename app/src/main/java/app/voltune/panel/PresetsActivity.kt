package app.voltune.panel

import android.os.Bundle
import android.widget.GridLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import app.voltune.panel.databinding.ActivityPresetsBinding
import app.voltune.panel.databinding.ItemPresetBinding

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
        buildGrid()
        highlight(store.load())
    }

    private fun buildGrid() {
        val current = store.load()
        val gap = dp(6)

        Presets.all.forEach { preset ->
            val card = ItemPresetBinding.inflate(layoutInflater, binding.presetGrid, false)
            card.presetName.setText(preset.name)
            card.presetPreview.interactive = false
            card.presetPreview.show(Presets.applyTo(current, preset))
            card.root.setOnClickListener { select(preset) }

            val params = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply {
                width = 0
                setMargins(gap, gap, gap, gap)
            }
            binding.presetGrid.addView(card.root, params)
            cards += preset to card
        }
    }

    private fun select(preset: Presets.Preset) {
        val updated = Presets.applyTo(store.load(), preset)
        store.save(updated)
        highlight(updated)

        val message = getString(R.string.preset_applied, getString(preset.name))
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

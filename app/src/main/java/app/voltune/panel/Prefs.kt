package app.voltune.panel

import android.content.Context
import androidx.core.content.edit

class Prefs(context: Context) {

    private val store = context.getSharedPreferences("voltune", Context.MODE_PRIVATE)

    var triggerOnLeft: Boolean
        get() = store.getBoolean(KEY_TRIGGER_LEFT, false)
        set(value) = store.edit { putBoolean(KEY_TRIGGER_LEFT, value) }

    var accentIndex: Int
        get() = store.getInt(KEY_ACCENT, 0).coerceIn(ACCENTS.indices)
        set(value) = store.edit { putInt(KEY_ACCENT, value) }

    var panelOpacity: Int
        get() = store.getInt(KEY_OPACITY, 100)
        set(value) = store.edit { putInt(KEY_OPACITY, value) }

    val accentColor: Int
        get() = ACCENTS[accentIndex]

    companion object {
        private const val KEY_TRIGGER_LEFT = "trigger_left"
        private const val KEY_ACCENT = "accent"
        private const val KEY_OPACITY = "panel_opacity"

        val ACCENTS = intArrayOf(
            0xFF6C5CE7.toInt(),
            0xFF00D1B2.toInt(),
            0xFFFF6B81.toInt(),
            0xFFFFB84D.toInt(),
            0xFF4DA3FF.toInt()
        )
    }
}

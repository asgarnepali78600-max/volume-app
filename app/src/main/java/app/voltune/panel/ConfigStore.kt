package app.voltune.panel

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONException
import org.json.JSONObject

class ConfigStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): PanelConfig {
        val raw = prefs.getString(KEY_CONFIG, null) ?: return PanelConfig()
        return try {
            PanelConfig.fromJson(JSONObject(raw))
        } catch (e: JSONException) {
            PanelConfig()
        }
    }

    fun save(config: PanelConfig) {
        prefs.edit { putString(KEY_CONFIG, config.toJson().toString()) }
    }

    fun update(change: (PanelConfig) -> PanelConfig): PanelConfig {
        val updated = change(load())
        save(updated)
        return updated
    }

    fun observe(listener: () -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        val handle = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_CONFIG) listener()
        }
        prefs.registerOnSharedPreferenceChangeListener(handle)
        return handle
    }

    fun stopObserving(handle: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(handle)
    }

    companion object {
        private const val FILE = "voltune_config"
        private const val KEY_CONFIG = "panel_config"
    }
}

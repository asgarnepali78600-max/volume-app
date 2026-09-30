package app.voltune.panel

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class PanelConfig(
    val layout: Layout = Layout.COLUMNS,
    val colors: Colors = Colors(),
    val panelCorner: Int = 28,
    val barCorner: Int = 20,
    val barThickness: Int = 48,
    val barLength: Int = 190,
    val barSpacing: Int = 10,
    val showIcons: Boolean = true,
    val showLevel: Boolean = true,
    val opacity: Int = 100,
    val streams: List<StreamEntry> = defaultStreams(),
    val trigger: Trigger = Trigger()
) {

    val enabledStreams: List<Stream>
        get() = streams.filter { it.enabled }.map { it.stream }

    enum class Layout(val key: String) {
        COLUMNS("columns"),
        ROWS("rows");

        companion object {
            fun fromKey(key: String): Layout? = entries.firstOrNull { it.key == key }
        }
    }

    data class StreamEntry(val stream: Stream, val enabled: Boolean)

    data class Colors(
        val panel: Int = 0xFF14161C.toInt(),
        val track: Int = 0xFF2A2E3A.toInt(),
        val fill: Int = 0xFF6C5CE7.toInt(),
        val icon: Int = 0xFFFFFFFF.toInt(),
        val text: Int = 0xFFE6E8EE.toInt()
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("panel", hex(panel))
            .put("track", hex(track))
            .put("fill", hex(fill))
            .put("icon", hex(icon))
            .put("text", hex(text))

        companion object {
            fun fromJson(json: JSONObject): Colors {
                val d = Colors()
                return Colors(
                    panel = json.optColor("panel", d.panel),
                    track = json.optColor("track", d.track),
                    fill = json.optColor("fill", d.fill),
                    icon = json.optColor("icon", d.icon),
                    text = json.optColor("text", d.text)
                )
            }
        }
    }

    data class Trigger(
        val onLeft: Boolean = false,
        val offset: Int = 0,
        val length: Int = 72,
        val thickness: Int = 6,
        val color: Int = 0xFF6C5CE7.toInt(),
        val alpha: Int = 210
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("onLeft", onLeft)
            .put("offset", offset)
            .put("length", length)
            .put("thickness", thickness)
            .put("color", hex(color))
            .put("alpha", alpha)

        companion object {
            fun fromJson(json: JSONObject): Trigger {
                val d = Trigger()
                return Trigger(
                    onLeft = json.optBoolean("onLeft", d.onLeft),
                    offset = json.optInt("offset", d.offset).coerceIn(-40, 40),
                    length = json.optInt("length", d.length).coerceIn(40, 160),
                    thickness = json.optInt("thickness", d.thickness).coerceIn(4, 16),
                    color = json.optColor("color", d.color),
                    alpha = json.optInt("alpha", d.alpha).coerceIn(60, 255)
                )
            }
        }
    }

    fun toJson(): JSONObject {
        val streamArray = JSONArray()
        streams.forEach {
            streamArray.put(JSONObject().put("key", it.stream.key).put("on", it.enabled))
        }

        return JSONObject()
            .put("version", VERSION)
            .put("layout", layout.key)
            .put("colors", colors.toJson())
            .put("panelCorner", panelCorner)
            .put("barCorner", barCorner)
            .put("barThickness", barThickness)
            .put("barLength", barLength)
            .put("barSpacing", barSpacing)
            .put("showIcons", showIcons)
            .put("showLevel", showLevel)
            .put("opacity", opacity)
            .put("streams", streamArray)
            .put("trigger", trigger.toJson())
    }

    companion object {
        const val VERSION = 1

        fun defaultStreams(): List<StreamEntry> {
            val onByDefault = setOf(Stream.MEDIA, Stream.RING, Stream.NOTIFICATION, Stream.ALARM)
            return Stream.entries.map { StreamEntry(it, it in onByDefault) }
        }

        fun fromJson(json: JSONObject): PanelConfig {
            val d = PanelConfig()
            return PanelConfig(
                layout = Layout.fromKey(json.optString("layout")) ?: d.layout,
                colors = json.optJSONObject("colors")?.let { Colors.fromJson(it) } ?: d.colors,
                panelCorner = json.optInt("panelCorner", d.panelCorner).coerceIn(0, 40),
                barCorner = json.optInt("barCorner", d.barCorner).coerceIn(0, 40),
                barThickness = json.optInt("barThickness", d.barThickness).coerceIn(24, 72),
                barLength = json.optInt("barLength", d.barLength).coerceIn(120, 280),
                barSpacing = json.optInt("barSpacing", d.barSpacing).coerceIn(0, 24),
                showIcons = json.optBoolean("showIcons", d.showIcons),
                showLevel = json.optBoolean("showLevel", d.showLevel),
                opacity = json.optInt("opacity", d.opacity).coerceIn(40, 100),
                streams = parseStreams(json.optJSONArray("streams")) ?: d.streams,
                trigger = json.optJSONObject("trigger")?.let { Trigger.fromJson(it) } ?: d.trigger
            )
        }

        private fun parseStreams(array: JSONArray?): List<StreamEntry>? {
            if (array == null) return null
            val result = mutableListOf<StreamEntry>()

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val stream = Stream.fromKey(item.optString("key")) ?: continue
                if (result.any { it.stream == stream }) continue
                result += StreamEntry(stream, item.optBoolean("on", true))
            }

            Stream.entries
                .filter { stream -> result.none { it.stream == stream } }
                .forEach { result += StreamEntry(it, false) }

            return result
        }
    }
}

private fun hex(color: Int): String = "#%08X".format(Locale.ROOT, color)

private fun JSONObject.optColor(name: String, fallback: Int): Int {
    val raw = optString(name)
    if (raw.isEmpty()) return fallback
    return try {
        Color.parseColor(raw)
    } catch (e: IllegalArgumentException) {
        fallback
    }
}

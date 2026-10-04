package app.voltune.panel

import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class PanelConfig(
    val layout: Layout = Layout.COLUMNS,
    val barStyle: BarStyle = BarStyle.SOLID,
    val colors: Colors = Colors(),
    val scale: Int = 100,
    val panelCorner: Int = 28,
    val barCorner: Int = 20,
    val barThickness: Int = 48,
    val barLength: Int = 190,
    val barSpacing: Int = 10,
    val barOutline: Int = 0,
    val borderWidth: Int = 0,
    val panelBackground: Boolean = true,
    val iconPosition: Placement = Placement.INSIDE,
    val levelPosition: Placement = Placement.INSIDE,
    val showIcons: Boolean = true,
    val showLevel: Boolean = true,
    val showLabels: Boolean = false,
    val animation: Animation = Animation.SLIDE,
    val position: Position = Position.CENTER,
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

    enum class BarStyle(val key: String) {
        SOLID("solid"),
        GRADIENT("gradient"),
        GLASS("glass"),
        SEGMENTED("segmented"),
        LINE("line");

        companion object {
            fun fromKey(key: String): BarStyle? = entries.firstOrNull { it.key == key }
        }
    }

    enum class Placement(val key: String) {
        INSIDE("inside"),
        START("start"),
        END("end");

        companion object {
            fun fromKey(key: String): Placement? = entries.firstOrNull { it.key == key }
        }
    }

    enum class Animation(val key: String) {
        NONE("none"),
        FADE("fade"),
        SLIDE("slide"),
        POP("pop");

        companion object {
            fun fromKey(key: String): Animation? = entries.firstOrNull { it.key == key }
        }
    }

    enum class Position(val key: String) {
        TOP("top"),
        CENTER("center"),
        BOTTOM("bottom");

        companion object {
            fun fromKey(key: String): Position? = entries.firstOrNull { it.key == key }
        }
    }

    data class StreamEntry(val stream: Stream, val enabled: Boolean)

    data class Colors(
        val panel: Int = 0xFF14161C.toInt(),
        val panelEnd: Int = 0xFF14161C.toInt(),
        val track: Int = 0xFF2A2E3A.toInt(),
        val fill: Int = 0xFF6C5CE7.toInt(),
        val fillEnd: Int = 0xFF9D8CFF.toInt(),
        val icon: Int = 0xFFFFFFFF.toInt(),
        val text: Int = 0xFFE6E8EE.toInt(),
        val border: Int = 0x33FFFFFF,
        val outline: Int = 0xFFFFFFFF.toInt()
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("panel", hex(panel))
            .put("panelEnd", hex(panelEnd))
            .put("track", hex(track))
            .put("fill", hex(fill))
            .put("fillEnd", hex(fillEnd))
            .put("icon", hex(icon))
            .put("text", hex(text))
            .put("border", hex(border))
            .put("outline", hex(outline))

        companion object {
            fun fromJson(json: JSONObject): Colors {
                val d = Colors()
                val panel = json.optColor("panel", d.panel)
                val fill = json.optColor("fill", d.fill)
                return Colors(
                    panel = panel,
                    panelEnd = json.optColor("panelEnd", panel),
                    track = json.optColor("track", d.track),
                    fill = fill,
                    fillEnd = json.optColor("fillEnd", fill),
                    icon = json.optColor("icon", d.icon),
                    text = json.optColor("text", d.text),
                    border = json.optColor("border", d.border),
                    outline = json.optColor("outline", d.outline)
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
            .put("barStyle", barStyle.key)
            .put("colors", colors.toJson())
            .put("scale", scale)
            .put("panelCorner", panelCorner)
            .put("barCorner", barCorner)
            .put("barThickness", barThickness)
            .put("barLength", barLength)
            .put("barSpacing", barSpacing)
            .put("barOutline", barOutline)
            .put("borderWidth", borderWidth)
            .put("panelBackground", panelBackground)
            .put("iconPosition", iconPosition.key)
            .put("levelPosition", levelPosition.key)
            .put("showIcons", showIcons)
            .put("showLevel", showLevel)
            .put("showLabels", showLabels)
            .put("animation", animation.key)
            .put("position", position.key)
            .put("opacity", opacity)
            .put("streams", streamArray)
            .put("trigger", trigger.toJson())
    }

    companion object {
        const val VERSION = 5

        fun defaultStreams(): List<StreamEntry> {
            val onByDefault = setOf(Stream.MEDIA, Stream.RING, Stream.NOTIFICATION, Stream.ALARM)
            return Stream.entries.map { StreamEntry(it, it in onByDefault) }
        }

        fun fromJson(json: JSONObject): PanelConfig {
            val d = PanelConfig()
            return PanelConfig(
                layout = Layout.fromKey(json.optString("layout")) ?: d.layout,
                barStyle = BarStyle.fromKey(json.optString("barStyle")) ?: d.barStyle,
                colors = json.optJSONObject("colors")?.let { Colors.fromJson(it) } ?: d.colors,
                scale = json.optInt("scale", d.scale).coerceIn(70, 150),
                panelCorner = json.optInt("panelCorner", d.panelCorner).coerceIn(0, 40),
                barCorner = json.optInt("barCorner", d.barCorner).coerceIn(0, 40),
                barThickness = json.optInt("barThickness", d.barThickness).coerceIn(24, 72),
                barLength = json.optInt("barLength", d.barLength).coerceIn(120, 280),
                barSpacing = json.optInt("barSpacing", d.barSpacing).coerceIn(0, 24),
                barOutline = json.optInt("barOutline", d.barOutline).coerceIn(0, 4),
                borderWidth = json.optInt("borderWidth", d.borderWidth).coerceIn(0, 4),
                panelBackground = json.optBoolean("panelBackground", d.panelBackground),
                iconPosition = placement(json.optString("iconPosition"), Placement.END),
                levelPosition = placement(json.optString("levelPosition"), Placement.START),
                showIcons = json.optBoolean("showIcons", d.showIcons),
                showLevel = json.optBoolean("showLevel", d.showLevel),
                showLabels = json.optBoolean("showLabels", d.showLabels),
                animation = Animation.fromKey(json.optString("animation")) ?: d.animation,
                position = Position.fromKey(json.optString("position")) ?: d.position,
                opacity = json.optInt("opacity", d.opacity).coerceIn(40, 100),
                streams = parseStreams(json.optJSONArray("streams")) ?: d.streams,
                trigger = json.optJSONObject("trigger")?.let { Trigger.fromJson(it) } ?: d.trigger
            )
        }

        private fun placement(key: String, legacyOutside: Placement): Placement =
            if (key == "outside") legacyOutside else Placement.fromKey(key) ?: Placement.INSIDE

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

package app.voltune.panel

import androidx.annotation.StringRes
import app.voltune.panel.PanelConfig.Animation
import app.voltune.panel.PanelConfig.BarStyle
import app.voltune.panel.PanelConfig.Colors
import app.voltune.panel.PanelConfig.Layout
import app.voltune.panel.PanelConfig.Placement
import app.voltune.panel.PanelConfig.Position
import app.voltune.panel.PanelConfig.StreamEntry

object Presets {

    data class Preset(
        @StringRes val name: Int,
        val look: PanelConfig,
        val premium: Boolean = false,
        val streams: List<Stream>? = null
    )

    enum class Part { ALL, LAYOUT, COLORS }

    val classic = listOf(
        Preset(R.string.preset_midnight, PanelConfig()),
        Preset(
            R.string.preset_arctic,
            PanelConfig(
                colors = colors(0xFFF4F6FA, 0xFFDDE3EC, 0xFF4DA3FF, 0xFFFFFFFF, 0xFF1E2230),
                barCorner = 24,
                barThickness = 52
            )
        ),
        Preset(
            R.string.preset_ember,
            PanelConfig(colors = colors(0xFF1A1210, 0xFF3A2622, 0xFFFF7A45, 0xFFFFFFFF, 0xFFFFD9C7))
        ),
        Preset(
            R.string.preset_mint,
            PanelConfig(
                layout = Layout.ROWS,
                colors = colors(0xFF0F1A17, 0xFF1F3029, 0xFF3DDC97, 0xFF0F1A17, 0xFFE9FFF6),
                barCorner = 12,
                barThickness = 40,
                barLength = 220
            )
        ),
        Preset(
            R.string.preset_sand,
            PanelConfig(colors = colors(0xFFF4EFE6, 0xFFE2D8C8, 0xFFB07D62, 0xFFFFFFFF, 0xFF3B2F25))
        ),
        Preset(
            R.string.preset_neon,
            PanelConfig(
                colors = colors(0xFF0A0A0F, 0xFF1A1A24, 0xFF00F5D4, 0xFF0A0A0F, 0xFF00F5D4),
                panelCorner = 16,
                barCorner = 8,
                barThickness = 40
            )
        ),
        Preset(
            R.string.preset_mono,
            PanelConfig(colors = colors(0xFF111111, 0xFF2B2B2B, 0xFFF2F2F2, 0xFF111111, 0xFFF2F2F2))
        ),
        Preset(
            R.string.preset_blush,
            PanelConfig(
                layout = Layout.ROWS,
                colors = colors(0xFFFFF1F3, 0xFFF7D6DC, 0xFFFF6B81, 0xFFFFFFFF, 0xFF7A2E3A),
                panelCorner = 20,
                barCorner = 14,
                barThickness = 44,
                barLength = 230
            )
        ),
        Preset(
            R.string.preset_ocean,
            PanelConfig(
                colors = colors(0xFF0B1622, 0xFF16293B, 0xFF4DA3FF, 0xFFFFFFFF, 0xFFE6E8EE),
                barCorner = 14,
                barThickness = 28,
                showLevel = false
            )
        ),
        Preset(
            R.string.preset_sunrise,
            PanelConfig(colors = colors(0xFF1C1530, 0xFF2E2448, 0xFFFFB84D, 0xFF1C1530, 0xFFFFB84D))
        )
    )

    val signature = listOf(
        Preset(
            R.string.preset_pocket,
            PanelConfig(
                barStyle = BarStyle.GRADIENT,
                colors = colors(
                    panel = 0xFF101218, track = 0xFF252936,
                    fill = 0xFF6C5CE7, fillEnd = 0xFF9D8CFF,
                    icon = 0xFFFFFFFF, text = 0xFFE6E8EE
                ),
                panelCorner = 24,
                barThickness = 40,
                barLength = 150,
                showLevel = false,
                animation = Animation.POP,
                position = Position.BOTTOM
            ),
            streams = listOf(Stream.MEDIA, Stream.RING)
        ),
        Preset(
            R.string.preset_floating_pills,
            PanelConfig(
                barStyle = BarStyle.GRADIENT,
                colors = colors(
                    panel = 0xFF14161C, track = 0xCC1E2230,
                    fill = 0xFF4DA3FF, fillEnd = 0xFF9D8CFF,
                    icon = 0xFFFFFFFF, text = 0xFFFFFFFF
                ),
                barCorner = 23,
                barThickness = 46,
                barLength = 200,
                panelBackground = false,
                iconPosition = Placement.END,
                levelPosition = Placement.START,
                animation = Animation.POP
            ),
            premium = true
        ),
        Preset(
            R.string.preset_studio_rows,
            PanelConfig(
                layout = Layout.ROWS,
                colors = colors(
                    panel = 0xFF15151A, track = 0xFF2A2A33,
                    fill = 0xFFFFB84D, icon = 0xFF15151A, text = 0xFFE6E6EA
                ),
                panelCorner = 18,
                barCorner = 8,
                barThickness = 30,
                barLength = 200,
                barSpacing = 12,
                showLabels = true,
                levelPosition = Placement.END
            )
        ),
        Preset(
            R.string.preset_dock,
            PanelConfig(
                barStyle = BarStyle.GLASS,
                colors = colors(
                    panel = 0xCC14161C, panelEnd = 0xCC1E2230,
                    track = 0x26FFFFFF, fill = 0xE6FFFFFF, fillEnd = 0x80B9C6FF,
                    icon = 0xFF1E2230, text = 0xFFFFFFFF, border = 0x33FFFFFF
                ),
                panelCorner = 32,
                barCorner = 16,
                barThickness = 56,
                barLength = 130,
                barSpacing = 12,
                borderWidth = 1,
                position = Position.BOTTOM
            ),
            streams = listOf(Stream.MEDIA, Stream.RING, Stream.ALARM)
        ),
        Preset(
            R.string.preset_halo,
            PanelConfig(
                barStyle = BarStyle.LINE,
                colors = colors(
                    panel = 0xFFFFFFFF, panelEnd = 0xFFF4F5F8,
                    track = 0xFFE3E6EC, fill = 0xFF6C5CE7, fillEnd = 0xFF4DA3FF,
                    icon = 0xFF6C5CE7, text = 0xFF3A3F4F
                ),
                barThickness = 44,
                barLength = 180,
                showLabels = true,
                levelPosition = Placement.START,
                animation = Animation.FADE
            )
        ),
        Preset(
            R.string.preset_capsule,
            PanelConfig(
                colors = colors(
                    panel = 0xFF2A2240, panelEnd = 0xFF3B2F5C,
                    track = 0xFFE9E3FF, fill = 0xFF6B4FD8,
                    icon = 0xFFFFFFFF, text = 0xFF2A2240, outline = 0xFFFFFFFF
                ),
                barCorner = 22,
                barThickness = 44,
                barOutline = 2,
                animation = Animation.POP
            ),
            premium = true
        ),
        Preset(
            R.string.preset_frost_line,
            PanelConfig(
                barStyle = BarStyle.LINE,
                colors = colors(
                    panel = 0xFFF7FAFF, panelEnd = 0xFFEAF1FB,
                    track = 0xFFD3DEEC, fill = 0xFF4DA3FF, fillEnd = 0xFF2E7BD6,
                    icon = 0xFF2E7BD6, text = 0xFF2E7BD6, border = 0x334DA3FF
                ),
                barThickness = 36,
                barLength = 200,
                borderWidth = 1,
                iconPosition = Placement.END,
                levelPosition = Placement.START
            ),
            premium = true
        ),
        Preset(
            R.string.preset_ledger,
            PanelConfig(
                layout = Layout.ROWS,
                barStyle = BarStyle.SEGMENTED,
                colors = colors(
                    panel = 0xFF0E0F12, track = 0xFF23252B,
                    fill = 0xFF3DDC97, fillEnd = 0xFF00D1B2,
                    icon = 0xFFE6E8EE, text = 0xFFE6E8EE
                ),
                panelCorner = 16,
                barCorner = 4,
                barThickness = 22,
                barLength = 180,
                showLabels = true,
                iconPosition = Placement.START,
                levelPosition = Placement.END
            ),
            premium = true
        )
    )

    val luxury = listOf(
        Preset(
            R.string.preset_royal_gold,
            PanelConfig(
                barStyle = BarStyle.GRADIENT,
                colors = colors(
                    panel = 0xFF0B0B0D, panelEnd = 0xFF1A1712,
                    track = 0xFF26221A, fill = 0xFFB8892B, fillEnd = 0xFFF7E7A1,
                    icon = 0xFF1A1206, text = 0xFFF7E7A1, border = 0x66D4AF37
                ),
                panelCorner = 26,
                barCorner = 18,
                borderWidth = 1
            ),
            premium = true
        ),
        Preset(
            R.string.preset_rose_gold,
            PanelConfig(
                barStyle = BarStyle.GRADIENT,
                colors = colors(
                    panel = 0xFFFFF6F4, panelEnd = 0xFFF3DCD7,
                    track = 0xFFEBD2CC, fill = 0xFFB76E79, fillEnd = 0xFFE8B4B8,
                    icon = 0xFFFFFFFF, text = 0xFF7A3E48, border = 0x55B76E79
                ),
                borderWidth = 1
            ),
            premium = true
        ),
        Preset(
            R.string.preset_liquid_glass,
            PanelConfig(
                barStyle = BarStyle.GLASS,
                colors = colors(
                    panel = 0xCC1B2030, panelEnd = 0xCC2A2146,
                    track = 0x26FFFFFF, fill = 0xE6FFFFFF, fillEnd = 0x80B9C6FF,
                    icon = 0xFF1E2230, text = 0xFFFFFFFF, border = 0x40FFFFFF
                ),
                panelCorner = 30,
                barCorner = 26,
                barThickness = 52,
                borderWidth = 1
            ),
            premium = true
        ),
        Preset(
            R.string.preset_aurora,
            PanelConfig(
                barStyle = BarStyle.GRADIENT,
                colors = colors(
                    panel = 0xFF120F2A, panelEnd = 0xFF0B2A33,
                    track = 0x22FFFFFF, fill = 0xFFFF6EC7, fillEnd = 0xFF4DE8FF,
                    icon = 0xFFFFFFFF, text = 0xFFE6F7FF
                ),
                barCorner = 22
            ),
            premium = true
        ),
        Preset(
            R.string.preset_obsidian,
            PanelConfig(
                barStyle = BarStyle.SEGMENTED,
                colors = colors(
                    panel = 0xFF050506, panelEnd = 0xFF16161A,
                    track = 0xFF222228, fill = 0xFFC0C0C8, fillEnd = 0xFFFFFFFF,
                    icon = 0xFF050506, text = 0xFFC0C0C8, border = 0x33FFFFFF
                ),
                panelCorner = 22,
                barCorner = 6,
                barThickness = 44,
                borderWidth = 1
            ),
            premium = true
        ),
        Preset(
            R.string.preset_cyber_neon,
            PanelConfig(
                barStyle = BarStyle.SEGMENTED,
                colors = colors(
                    panel = 0xFF07060D, panelEnd = 0xFF100C1F,
                    track = 0xFF1B1830, fill = 0xFFFF2BD6, fillEnd = 0xFF00F0FF,
                    icon = 0xFF07060D, text = 0xFF00F0FF, border = 0x6600F0FF
                ),
                panelCorner = 14,
                barCorner = 4,
                barThickness = 40,
                borderWidth = 1
            ),
            premium = true
        ),
        Preset(
            R.string.preset_velvet,
            PanelConfig(
                barStyle = BarStyle.GRADIENT,
                colors = colors(
                    panel = 0xFF2B0A14, panelEnd = 0xFF12040A,
                    track = 0xFF3D1220, fill = 0xFF8E1B3A, fillEnd = 0xFFE05A7A,
                    icon = 0xFFFFFFFF, text = 0xFFF4C7D2, border = 0x44E05A7A
                ),
                borderWidth = 1
            ),
            premium = true
        ),
        Preset(
            R.string.preset_emerald_lux,
            PanelConfig(
                barStyle = BarStyle.GRADIENT,
                colors = colors(
                    panel = 0xFF06140F, panelEnd = 0xFF0F241B,
                    track = 0xFF173528, fill = 0xFF0F7B55, fillEnd = 0xFFD4AF37,
                    icon = 0xFFFFFFFF, text = 0xFFF7E7A1, border = 0x55D4AF37
                ),
                borderWidth = 1
            ),
            premium = true
        ),
        Preset(
            R.string.preset_minimal_line,
            PanelConfig(
                barStyle = BarStyle.LINE,
                colors = colors(
                    panel = 0xFFFAFAF7, panelEnd = 0xFFF1F0EC,
                    track = 0xFFE4E2DC, fill = 0xFF1C1C1E, fillEnd = 0xFF3A3A3C,
                    icon = 0xFF1C1C1E, text = 0xFF6B6B70
                ),
                panelCorner = 24,
                barThickness = 40
            ),
            premium = true
        ),
        Preset(
            R.string.preset_sunset_glow,
            PanelConfig(
                barStyle = BarStyle.GRADIENT,
                colors = colors(
                    panel = 0xFF1E1024, panelEnd = 0xFF2A0F1A,
                    track = 0x22FFFFFF, fill = 0xFFFF7A45, fillEnd = 0xFFFF3D8B,
                    icon = 0xFFFFFFFF, text = 0xFFFFD1C2
                ),
                barCorner = 24
            ),
            premium = true
        )
    )

    fun apply(current: PanelConfig, preset: Preset, part: Part): PanelConfig = when (part) {
        Part.ALL -> applyColors(applyLayout(current, preset), preset)
        Part.LAYOUT -> applyLayout(current, preset)
        Part.COLORS -> applyColors(current, preset)
    }

    fun isActive(current: PanelConfig, preset: Preset): Boolean =
        apply(current, preset, Part.ALL) == current

    private fun applyLayout(current: PanelConfig, preset: Preset): PanelConfig {
        val look = preset.look
        return current.copy(
            layout = look.layout,
            barStyle = look.barStyle,
            panelCorner = look.panelCorner,
            barCorner = look.barCorner,
            barThickness = look.barThickness,
            barLength = look.barLength,
            barSpacing = look.barSpacing,
            barOutline = look.barOutline,
            borderWidth = look.borderWidth,
            panelBackground = look.panelBackground,
            iconPosition = look.iconPosition,
            levelPosition = look.levelPosition,
            showIcons = look.showIcons,
            showLevel = look.showLevel,
            showLabels = look.showLabels,
            animation = look.animation,
            position = look.position,
            opacity = look.opacity,
            streams = preset.streams?.let { withStreams(current.streams, it) } ?: current.streams
        )
    }

    private fun applyColors(current: PanelConfig, preset: Preset): PanelConfig =
        current.copy(
            colors = preset.look.colors,
            trigger = current.trigger.copy(color = preset.look.colors.fill)
        )

    private fun withStreams(current: List<StreamEntry>, wanted: List<Stream>): List<StreamEntry> {
        val first = wanted.map { StreamEntry(it, true) }
        val rest = current.map { it.stream }
            .filter { it !in wanted }
            .map { StreamEntry(it, false) }
        return first + rest
    }

    private fun colors(
        panel: Long,
        track: Long,
        fill: Long,
        icon: Long,
        text: Long,
        panelEnd: Long = panel,
        fillEnd: Long = fill,
        border: Long = 0x33FFFFFF,
        outline: Long = 0xFFFFFFFF
    ) = Colors(
        panel = panel.toInt(),
        panelEnd = panelEnd.toInt(),
        track = track.toInt(),
        fill = fill.toInt(),
        fillEnd = fillEnd.toInt(),
        icon = icon.toInt(),
        text = text.toInt(),
        border = border.toInt(),
        outline = outline.toInt()
    )
}

package app.voltune.panel

import androidx.annotation.StringRes
import app.voltune.panel.PanelConfig.Colors
import app.voltune.panel.PanelConfig.Layout

object Presets {

    data class Preset(@StringRes val name: Int, val look: PanelConfig)

    val all = listOf(
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

    fun applyTo(current: PanelConfig, preset: Preset): PanelConfig {
        val look = preset.look
        return current.copy(
            layout = look.layout,
            colors = look.colors,
            panelCorner = look.panelCorner,
            barCorner = look.barCorner,
            barThickness = look.barThickness,
            barLength = look.barLength,
            barSpacing = look.barSpacing,
            showIcons = look.showIcons,
            showLevel = look.showLevel,
            opacity = look.opacity,
            trigger = current.trigger.copy(color = look.colors.fill)
        )
    }

    fun isActive(current: PanelConfig, preset: Preset): Boolean =
        applyTo(current, preset) == current

    private fun colors(panel: Long, track: Long, fill: Long, icon: Long, text: Long) = Colors(
        panel = panel.toInt(),
        track = track.toInt(),
        fill = fill.toInt(),
        icon = icon.toInt(),
        text = text.toInt()
    )
}

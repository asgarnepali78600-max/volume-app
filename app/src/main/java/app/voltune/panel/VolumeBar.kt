package app.voltune.panel

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import app.voltune.panel.PanelConfig.BarStyle
import kotlin.math.min
import kotlin.math.roundToInt

class VolumeBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var style = BarStyle.SOLID
        set(value) { field = value; shadersDirty = true; invalidate() }

    var vertical = true
        set(value) { field = value; shadersDirty = true; invalidate() }

    var cornerRadius = dp(20f)
        set(value) { field = value; invalidate() }

    var showLevel = true
        set(value) { field = value; invalidate() }

    var levelText = ""
        set(value) { field = value; invalidate() }

    var haptics = true

    var icon: Drawable? = null
        set(value) { field = value?.mutate(); invalidate() }

    var min = 0
        private set
    var max = 1
        private set
    var level = 0
        private set

    var onUserChange: ((Int) -> Unit)? = null
    var onTouchStart: (() -> Unit)? = null
    var onTouchEnd: (() -> Unit)? = null

    private var trackColor = 0xFF2A2E3A.toInt()
    private var fillColor = 0xFF6C5CE7.toInt()
    private var fillEndColor = 0xFF9D8CFF.toInt()
    private var iconColor = 0xFFFFFFFF.toInt()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glossPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x59FFFFFF
    }
    private val lineTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val lineFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val knobEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x66FFFFFF
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
    }

    private val bounds = RectF()
    private val segment = RectF()
    private val clip = Path()
    private var shadersDirty = true

    fun setColors(track: Int, fill: Int, fillEnd: Int, icon: Int, text: Int) {
        trackColor = track
        fillColor = fill
        fillEndColor = fillEnd
        iconColor = icon
        trackPaint.color = track
        fillPaint.color = fill
        lineTrackPaint.color = track
        knobPaint.color = fillEnd
        textPaint.color = text
        shadersDirty = true
        invalidate()
    }

    fun setRange(min: Int, max: Int) {
        this.min = min
        this.max = if (max > min) max else min + 1
        level = level.coerceIn(this.min, this.max)
        invalidate()
    }

    fun setLevel(value: Int) {
        val clamped = value.coerceIn(min, max)
        if (clamped == level) return
        level = clamped
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        shadersDirty = true
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        if (shadersDirty) buildShaders(w, h)

        when (style) {
            BarStyle.SOLID, BarStyle.GRADIENT, BarStyle.GLASS -> drawFilled(canvas, w, h)
            BarStyle.SEGMENTED -> drawSegments(canvas, w, h)
            BarStyle.LINE -> drawLine(canvas, w, h)
        }

        drawIcon(canvas, w, h)
        if (showLevel && levelText.isNotEmpty()) drawLevel(canvas, w, h)
    }

    private fun fraction() = (level - min).toFloat() / (max - min)

    private fun buildShaders(w: Float, h: Float) {
        val gradient = if (style == BarStyle.SOLID) {
            null
        } else if (vertical) {
            LinearGradient(0f, h, 0f, 0f, fillColor, fillEndColor, Shader.TileMode.CLAMP)
        } else {
            LinearGradient(0f, 0f, w, 0f, fillColor, fillEndColor, Shader.TileMode.CLAMP)
        }
        fillPaint.shader = gradient
        lineFillPaint.shader = gradient
        lineFillPaint.color = fillColor

        glossPaint.shader = if (vertical) {
            LinearGradient(0f, 0f, w * 0.6f, 0f, 0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        } else {
            LinearGradient(0f, 0f, 0f, h * 0.6f, 0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        }
        shadersDirty = false
    }

    private fun drawFilled(canvas: Canvas, w: Float, h: Float) {
        bounds.set(0f, 0f, w, h)
        val radius = min(cornerRadius, min(w, h) / 2f)
        clip.reset()
        clip.addRoundRect(bounds, radius, radius, Path.Direction.CW)

        val f = fraction()
        canvas.save()
        canvas.clipPath(clip)
        canvas.drawRect(bounds, trackPaint)
        if (vertical) {
            canvas.drawRect(0f, h - h * f, w, h, fillPaint)
        } else {
            canvas.drawRect(0f, 0f, w * f, h, fillPaint)
        }
        if (style == BarStyle.GLASS) canvas.drawRect(bounds, glossPaint)
        canvas.restore()

        if (style == BarStyle.GLASS) {
            val stroke = dp(1f)
            edgePaint.strokeWidth = stroke
            bounds.inset(stroke / 2f, stroke / 2f)
            canvas.drawRoundRect(bounds, radius, radius, edgePaint)
        }
    }

    private fun drawSegments(canvas: Canvas, w: Float, h: Float) {
        val count = (max - min).coerceIn(4, 12)
        val gap = dp(3f)
        val length = if (vertical) h else w
        val size = (length - gap * (count - 1)) / count
        val filled = (fraction() * count).roundToInt()
        val radius = min(cornerRadius, min(size, if (vertical) w else h) / 2f)

        for (i in 0 until count) {
            val start = i * (size + gap)
            if (vertical) {
                segment.set(0f, h - start - size, w, h - start)
            } else {
                segment.set(start, 0f, start + size, h)
            }
            segmentPaint.color = if (i < filled) {
                ColorUtils.blendARGB(fillColor, fillEndColor, i / (count - 1f))
            } else {
                trackColor
            }
            canvas.drawRoundRect(segment, radius, radius, segmentPaint)
        }
    }

    private fun drawLine(canvas: Canvas, w: Float, h: Float) {
        val (start, end) = lineRange(w, h)
        val stroke = dp(4f)
        lineTrackPaint.strokeWidth = stroke
        lineFillPaint.strokeWidth = stroke

        val position = start + (end - start) * fraction()
        val knobRadius = min(dp(10f), min(w, h) * 0.28f)
        knobEdgePaint.strokeWidth = dp(2f)

        if (vertical) {
            val cx = w / 2f
            canvas.drawLine(cx, start, cx, end, lineTrackPaint)
            canvas.drawLine(cx, start, cx, position, lineFillPaint)
            canvas.drawCircle(cx, position, knobRadius, knobPaint)
            canvas.drawCircle(cx, position, knobRadius, knobEdgePaint)
        } else {
            val cy = h / 2f
            canvas.drawLine(start, cy, end, cy, lineTrackPaint)
            canvas.drawLine(start, cy, position, cy, lineFillPaint)
            canvas.drawCircle(position, cy, knobRadius, knobPaint)
            canvas.drawCircle(position, cy, knobRadius, knobEdgePaint)
        }
    }

    private fun lineRange(w: Float, h: Float): Pair<Float, Float> {
        val pad = dp(12f)
        val hasText = showLevel && levelText.isNotEmpty()
        return if (vertical) {
            val bottom = if (icon != null) h - w else h - pad
            val top = if (hasText) w else pad
            bottom to top
        } else {
            val left = if (icon != null) h else pad
            val right = if (hasText) w - dp(44f) else w - pad
            left to right
        }
    }

    private fun drawIcon(canvas: Canvas, w: Float, h: Float) {
        val drawable = icon ?: return
        val size = min(dp(22f), min(w, h) * 0.5f)
        val cx = if (vertical) w / 2f else h / 2f
        val cy = if (vertical) h - w / 2f else h / 2f
        val half = size / 2f

        drawable.setBounds(
            (cx - half).roundToInt(),
            (cy - half).roundToInt(),
            (cx + half).roundToInt(),
            (cy + half).roundToInt()
        )
        drawable.setTint(iconColor)
        drawable.draw(canvas)
    }

    private fun drawLevel(canvas: Canvas, w: Float, h: Float) {
        textPaint.textSize = min(dp(12f), min(w, h) * 0.3f)
        val centerOffset = (textPaint.descent() + textPaint.ascent()) / 2f

        if (vertical) {
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(levelText, w / 2f, w / 2f - centerOffset, textPaint)
        } else {
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(levelText, w - dp(14f), h / 2f - centerOffset, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                onTouchStart?.invoke()
                updateFromTouch(event)
            }
            MotionEvent.ACTION_MOVE -> updateFromTouch(event)
            MotionEvent.ACTION_UP -> {
                performClick()
                finishTouch()
            }
            MotionEvent.ACTION_CANCEL -> finishTouch()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun finishTouch() {
        parent?.requestDisallowInterceptTouchEvent(false)
        onTouchEnd?.invoke()
    }

    private fun updateFromTouch(event: MotionEvent) {
        val fraction = if (style == BarStyle.LINE) {
            val (start, end) = lineRange(width.toFloat(), height.toFloat())
            val position = if (vertical) event.y else event.x
            if (end == start) 0f else (position - start) / (end - start)
        } else if (vertical) {
            1f - event.y / height
        } else {
            event.x / width
        }

        val target = min + ((max - min) * fraction.coerceIn(0f, 1f)).roundToInt()
        if (target == level) return

        level = target
        invalidate()
        if (haptics) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onUserChange?.invoke(target)
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
}

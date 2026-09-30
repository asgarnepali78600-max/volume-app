package app.voltune.panel

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.min
import kotlin.math.roundToInt

class VolumeBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var vertical = true
        set(value) { field = value; invalidate() }

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

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
    }
    private var iconColor = 0xFFFFFFFF.toInt()

    private val bounds = RectF()
    private val clip = Path()

    fun setColors(track: Int, fill: Int, icon: Int, text: Int) {
        trackPaint.color = track
        fillPaint.color = fill
        iconColor = icon
        textPaint.color = text
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

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        bounds.set(0f, 0f, w, h)
        val radius = min(cornerRadius, min(w, h) / 2f)
        clip.reset()
        clip.addRoundRect(bounds, radius, radius, Path.Direction.CW)

        val fraction = (level - min).toFloat() / (max - min)

        canvas.save()
        canvas.clipPath(clip)
        canvas.drawRect(bounds, trackPaint)
        if (vertical) {
            canvas.drawRect(0f, h - h * fraction, w, h, fillPaint)
        } else {
            canvas.drawRect(0f, 0f, w * fraction, h, fillPaint)
        }
        canvas.restore()

        drawIcon(canvas, w, h)
        if (showLevel && levelText.isNotEmpty()) drawLevel(canvas, w, h)
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
        val fraction = if (vertical) 1f - event.y / height else event.x / width
        val target = min + ((max - min) * fraction.coerceIn(0f, 1f)).roundToInt()
        if (target == level) return

        level = target
        invalidate()
        if (haptics) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        onUserChange?.invoke(target)
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
}

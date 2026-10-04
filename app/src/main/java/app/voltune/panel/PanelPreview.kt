package app.voltune.panel

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import kotlin.math.max

class PanelPreview @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    val panel = PanelView(context)

    init {
        addView(panel, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    }

    fun show(config: PanelConfig) {
        panel.apply(config)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val unspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        panel.measure(unspecified, unspecified)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val available = width - paddingLeft - paddingRight
        val scale = if (available > 0 && panel.measuredWidth > available) {
            available.toFloat() / panel.measuredWidth
        } else {
            1f
        }
        panel.scaleX = scale
        panel.scaleY = scale

        val contentHeight = (panel.measuredHeight * scale).toInt() + paddingTop + paddingBottom
        val height = resolveSize(max(contentHeight, suggestedMinimumHeight), heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val cx = width / 2
        val cy = height / 2
        val halfW = panel.measuredWidth / 2
        val halfH = panel.measuredHeight / 2
        panel.layout(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
    }
}

package app.voltune.panel

import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import app.voltune.panel.PanelConfig.Animation

object PanelAnimator {

    fun enter(view: ViewGroup, animation: Animation, fromLeft: Boolean) {
        val targetAlpha = view.alpha
        val side = if (fromLeft) -1f else 1f
        val shift = 48 * view.resources.displayMetrics.density

        when (animation) {
            Animation.NONE -> Unit
            Animation.FADE -> {
                view.alpha = 0f
                view.animate().alpha(targetAlpha).setDuration(180).start()
            }
            Animation.SLIDE -> {
                view.alpha = 0f
                view.translationX = side * shift
                view.animate()
                    .alpha(targetAlpha)
                    .translationX(0f)
                    .setDuration(240)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
                cascade(view) { child -> child.translationX = side * shift / 3f }
            }
            Animation.POP -> {
                view.alpha = 0f
                view.scaleX = 0.8f
                view.scaleY = 0.8f
                view.animate()
                    .alpha(targetAlpha)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(280)
                    .setInterpolator(OvershootInterpolator(1.6f))
                    .start()
                cascade(view) { child ->
                    child.scaleX = 0.6f
                    child.scaleY = 0.6f
                }
            }
        }
    }

    fun exit(view: View, animation: Animation, onEnd: () -> Unit) {
        if (animation == Animation.NONE) {
            onEnd()
            return
        }
        view.animate()
            .alpha(0f)
            .setDuration(120)
            .withEndAction(onEnd)
            .start()
    }

    private fun cascade(view: ViewGroup, prepare: (View) -> Unit) {
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            child.alpha = 0f
            prepare(child)
            child.animate()
                .alpha(1f)
                .translationX(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(60L + i * 35L)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
}

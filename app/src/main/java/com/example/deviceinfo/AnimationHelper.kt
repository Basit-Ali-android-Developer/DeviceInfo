package com.example.deviceinfo

import android.animation.ValueAnimator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.animation.doOnEnd


object AnimationHelper {

    // 🔹 Animate once at fragment startup (0 → 100 → finalValue)
    fun animateIntroProgress(progressBar: ProgressBar, textView: TextView?, finalValue: Int) {
        val animatorUp = ValueAnimator.ofInt(0, 100)
        animatorUp.duration = 1500

        val animatorDown = ValueAnimator.ofInt(100, finalValue)
        animatorDown.duration = 1500

        animatorUp.addUpdateListener {
            val value = it.animatedValue as Int
            progressBar.progress = value
            textView?.text = "$value%"
        }

        animatorDown.addUpdateListener {
            val value = it.animatedValue as Int
            progressBar.progress = value
            textView?.text = "$value%"
        }

        animatorUp.doOnEnd { animatorDown.start() }
        animatorUp.start()
    }
    // 🔹 Simple fade-in + scale animation (optional for icons, views)
    fun animateAppear(view: android.view.View, duration: Long = 800) {
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.alpha = 0f
        view.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(duration)
            .start()
    }
}

package app.tostrr.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.scene.Scene

/**
 * Screen transitions of the main [androidx.navigation3.ui.NavDisplay]. A new screen slides in from the right over
 * the current one, which drifts left and fades; going back plays it in reverse. The predictive back gesture uses the
 * same pop animation, driven by the swipe, so the detail follows the finger instead of shrinking and vanishing.
 */
object Transitions {
    private const val DURATION_MS = 300

    /** How far the screen underneath drifts, as a fraction of the width. */
    private const val PARALLAX = 4

    private fun <T> spec() = tween<T>(DURATION_MS, easing = FastOutSlowInEasing)

    fun <T : Any> push(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        slideInHorizontally(spec()) { it } togetherWith
            (slideOutHorizontally(spec()) { -it / PARALLAX } + fadeOut(spec()))
    }

    fun <T : Any> pop(): AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        (slideInHorizontally(spec()) { -it / PARALLAX } + fadeIn(spec())) togetherWith
            slideOutHorizontally(spec()) { it }
    }

    fun <T : Any> predictivePop(): AnimatedContentTransitionScope<Scene<T>>.(Int) -> ContentTransform = { pop<T>()() }
}

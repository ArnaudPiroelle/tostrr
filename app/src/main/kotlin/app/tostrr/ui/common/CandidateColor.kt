package app.tostrr.ui.common

import androidx.compose.ui.graphics.Color

/**
 * The colour of the [index]th ship that may carry the car, the same on the globe and in the list under it: the first
 * in [accent], the others in colours that stand out on the night globe.
 */
internal fun candidateColor(index: Int, accent: Color): Color =
    if (index == 0) accent else CandidateColors[(index - 1) % CandidateColors.size]

private val CandidateColors = listOf(Color(0xFFFFB547), Color(0xFF4FD1E8), Color(0xFFB18CFF), Color(0xFF7EDC9E))

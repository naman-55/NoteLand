package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.util.Calendar
import kotlinx.coroutines.delay

/**
 * Computes milliseconds remaining until the start of the next wall-clock hour (+50ms buffer),
 * allowing time-of-day UIs to suspend cleanly instead of polling on a short interval.
 */
fun millisUntilNextHour(nowMillis: Long = System.currentTimeMillis()): Long {
    val nextHour = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 50)
        add(Calendar.HOUR_OF_DAY, 1)
    }
    return (nextHour.timeInMillis - nowMillis).coerceAtLeast(1_000L)
}

/**
 * Returns the current hour of day [0..23], updating immediately on foreground resume
 * and sleeping until the next hour boundary while the UI is STARTED.
 * Produces zero background wakeups when the screen is STOPPED.
 */
@Composable
fun rememberLifecycleCurrentHour(): Int {
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentHour by remember {
        mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = System.currentTimeMillis()
                currentHour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
                delay(millisUntilNextHour(now))
            }
        }
    }

    return currentHour
}

/**
 * Allocation-free check for whether comma-separated [tagsCsv] contains exact trimmed [targetTag].
 */
fun noteHasTag(tagsCsv: String, targetTag: String): Boolean {
    if (tagsCsv.isBlank() || targetTag.isBlank()) return false
    var start = 0
    val len = tagsCsv.length
    while (start <= len) {
        val commaIdx = tagsCsv.indexOf(',', start)
        val end = if (commaIdx == -1) len else commaIdx
        var tokenStart = start
        var tokenEnd = end
        while (tokenStart < tokenEnd && tagsCsv[tokenStart].isWhitespace()) tokenStart++
        while (tokenEnd > tokenStart && tagsCsv[tokenEnd - 1].isWhitespace()) tokenEnd--
        if (tokenEnd - tokenStart == targetTag.length &&
            tagsCsv.regionMatches(tokenStart, targetTag, 0, targetTag.length, ignoreCase = false)
        ) {
            return true
        }
        if (commaIdx == -1) break
        start = commaIdx + 1
    }
    return false
}

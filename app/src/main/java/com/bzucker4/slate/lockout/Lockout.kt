package com.bzucker4.slate.lockout

/** Lockout duration options and active-session checks. */
object Lockout {
    fun formatCountdown(remainingMs: Long): String {
        val totalSeconds = (remainingMs.coerceAtLeast(0L) / 1000L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }
}

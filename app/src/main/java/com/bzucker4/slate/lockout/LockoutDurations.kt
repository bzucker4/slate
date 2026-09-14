package com.bzucker4.slate.lockout

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object LockoutDurations {
    const val THIRTY_MINUTES_MS = 30L * 60L * 1000L
    const val TWO_HOURS_MS = 2L * 60L * 60L * 1000L
    val MORNING_UNLOCK: LocalTime = LocalTime.of(7, 0)

    fun millisUntilNextMorning(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val now = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId)
        var target: ZonedDateTime = now.toLocalDate().atTime(MORNING_UNLOCK).atZone(zoneId)
        if (!now.isBefore(target)) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target).toMillis().coerceAtLeast(0L)
    }

    const val UNTIL_MORNING_SELECTION = -1L

    fun optionForStoredDuration(selectedDurationMs: Long): DurationOption {
        return when (selectedDurationMs) {
            THIRTY_MINUTES_MS -> DurationOption.ThirtyMinutes
            TWO_HOURS_MS -> DurationOption.TwoHours
            UNTIL_MORNING_SELECTION -> DurationOption.UntilMorning
            else -> if (selectedDurationMs <= 0L) DurationOption.TwoHours else DurationOption.UntilMorning
        }
    }
}

enum class DurationOption {
    ThirtyMinutes,
    TwoHours,
    UntilMorning,
    ;

    fun durationMs(nowEpochMs: Long = System.currentTimeMillis()): Long =
        when (this) {
            ThirtyMinutes -> LockoutDurations.THIRTY_MINUTES_MS
            TwoHours -> LockoutDurations.TWO_HOURS_MS
            UntilMorning -> LockoutDurations.millisUntilNextMorning(nowEpochMs)
        }

    fun storedSelectionMs(): Long =
        when (this) {
            ThirtyMinutes -> LockoutDurations.THIRTY_MINUTES_MS
            TwoHours -> LockoutDurations.TWO_HOURS_MS
            UntilMorning -> LockoutDurations.UNTIL_MORNING_SELECTION
        }
}

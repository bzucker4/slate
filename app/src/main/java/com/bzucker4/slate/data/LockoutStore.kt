package com.bzucker4.slate.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bzucker4.slate.lockout.LockoutDurations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lockoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "lockout")

data class LockoutSnapshot(
    val isLockedOut: Boolean = false,
    val lockoutEndsAtEpochMs: Long = 0L,
    val selectedDurationMs: Long = LockoutDurations.TWO_HOURS_MS,
) {
    fun isActive(nowEpochMs: Long): Boolean =
        isLockedOut && nowEpochMs < lockoutEndsAtEpochMs
}

class LockoutStore(context: Context) {
    private val dataStore = context.applicationContext.lockoutDataStore

    val snapshot: Flow<LockoutSnapshot> = dataStore.data.map { prefs ->
        LockoutSnapshot(
            isLockedOut = prefs[Keys.IS_LOCKED_OUT] ?: false,
            lockoutEndsAtEpochMs = prefs[Keys.LOCKOUT_ENDS_AT_EPOCH_MS] ?: 0L,
            selectedDurationMs = prefs[Keys.SELECTED_DURATION_MS]
                ?: LockoutDurations.TWO_HOURS_MS,
        )
    }

    suspend fun setSelectedDurationMs(durationMs: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.SELECTED_DURATION_MS] = durationMs
        }
    }

    suspend fun beginLockout(
        durationMs: Long,
        selectedDurationMs: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_LOCKED_OUT] = true
            prefs[Keys.LOCKOUT_ENDS_AT_EPOCH_MS] = nowEpochMs + durationMs
            prefs[Keys.SELECTED_DURATION_MS] = selectedDurationMs
        }
    }

    suspend fun clearExpiredLockout(nowEpochMs: Long = System.currentTimeMillis()) {
        dataStore.edit { prefs ->
            val locked = prefs[Keys.IS_LOCKED_OUT] ?: false
            val endsAt = prefs[Keys.LOCKOUT_ENDS_AT_EPOCH_MS] ?: 0L
            if (locked && nowEpochMs >= endsAt) {
                prefs[Keys.IS_LOCKED_OUT] = false
            }
        }
    }

    private object Keys {
        val IS_LOCKED_OUT = booleanPreferencesKey("isLockedOut")
        val LOCKOUT_ENDS_AT_EPOCH_MS = longPreferencesKey("lockoutEndsAtEpochMs")
        val SELECTED_DURATION_MS = longPreferencesKey("selectedDurationMs")
    }
}

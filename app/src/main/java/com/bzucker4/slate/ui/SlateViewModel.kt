package com.bzucker4.slate.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bzucker4.slate.audio.SlateAudio
import com.bzucker4.slate.data.LockoutSnapshot
import com.bzucker4.slate.data.LockoutStore
import com.bzucker4.slate.lockout.DurationOption
import com.bzucker4.slate.lockout.LockoutDurations
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SlateUiState(
    val storeLoaded: Boolean = false,
    val nowEpochMs: Long = System.currentTimeMillis(),
    val snapshot: LockoutSnapshot = LockoutSnapshot(),
    val selectedOption: DurationOption = DurationOption.TwoHours,
    val scratching: Boolean = false,
) {
    val isLockedOutActive: Boolean get() = snapshot.isActive(nowEpochMs)
    val remainingMs: Long
        get() = (snapshot.lockoutEndsAtEpochMs - nowEpochMs).coerceAtLeast(0L)
}

class SlateViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LockoutStore(application)
    private val audio = SlateAudio(application)
    private val nowEpochMs = MutableStateFlow(System.currentTimeMillis())
    private val selectedOptionOverride = MutableStateFlow<DurationOption?>(null)
    private val scratching = MutableStateFlow(false)

    val uiState: StateFlow<SlateUiState> = combine(
        store.snapshot,
        nowEpochMs,
        selectedOptionOverride,
        scratching,
    ) { snapshot, now, override, isScratching ->
        SlateUiState(
            storeLoaded = true,
            nowEpochMs = now,
            snapshot = snapshot,
            selectedOption = override
                ?: LockoutDurations.optionForStoredDuration(snapshot.selectedDurationMs),
            scratching = isScratching,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SlateUiState(),
    )

    init {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                nowEpochMs.value = now
                store.clearExpiredLockout(now)
                delay(1_000)
            }
        }
    }

    fun selectDuration(option: DurationOption) {
        selectedOptionOverride.value = option
        viewModelScope.launch {
            store.setSelectedDurationMs(option.storedSelectionMs())
        }
    }

    fun begin() {
        scratching.value = true
    }

    fun onScrubMove(speedPxPerMs: Float) {
        audio.startOrUpdateScrub(speedPxPerMs)
    }

    fun onScrubStop() {
        audio.stopScrub()
    }

    /**
     * Short sub-bass hit. Call when frost dissolve finishes.
     * Dissolve is not implemented yet; this is the hook for that chunk.
     */
    fun playCompletionChime() {
        audio.playCompletionChime()
    }

    fun onScratchCleared() {
        audio.stopScrub()
        val option = uiState.value.selectedOption
        viewModelScope.launch {
            store.beginLockout(
                durationMs = option.durationMs(),
                selectedDurationMs = option.storedSelectionMs(),
            )
            scratching.value = false
        }
    }

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }
}

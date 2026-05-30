package com.phasma.ghostdetector.ui.detector

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.phasma.ghostdetector.ar.GhostEvent

data class DetectorState(
    val cameraPermitted: Boolean = false,
    val signalLevel: Float = 0f,
    val statusText: String = "Calibrating spectral sensors…",
    val events: List<GhostEvent> = emptyList()
)

class DetectorViewModel : ViewModel() {
    var state by mutableStateOf(DetectorState())
        private set

    fun setPermission(granted: Boolean) {
        state = state.copy(cameraPermitted = granted)
    }

    fun setSignal(level: Float) {
        state = state.copy(signalLevel = level)
    }

    fun setStatus(text: String) {
        state = state.copy(statusText = text)
    }

    fun logEvent(event: GhostEvent) {
        val newList = (listOf(event) + state.events).take(8)
        state = state.copy(events = newList)
    }
}

package com.example.rideboard

import com.example.rideboard.buffer.GpsSample
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * AppEvents est un bus global qui permet d'envoyer des événements
 * depuis le service vers le ViewModel (par ex : nouveaux points GPS)
 */
object AppEvents {
    private val _gpsSamplesFlow = MutableSharedFlow<GpsSample>(replay = 0)
    val gpsSamplesFlow = _gpsSamplesFlow.asSharedFlow()

    suspend fun emitGpsSample(sample: GpsSample) {
        _gpsSamplesFlow.emit(sample)
    }
}

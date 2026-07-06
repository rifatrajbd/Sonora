package com.sonora.music.core.model

/** Lightweight UI-state wrapper for loading / empty / error / content states. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data object Empty : UiState<Nothing>
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>
}

inline fun <T> UiState<T>.onSuccess(block: (T) -> Unit): UiState<T> {
    if (this is UiState.Success) block(data)
    return this
}

package io.github.jolkert.cobblebreeding.mixinkt

data class CancellableResult<T>(val value: T?, val shouldCancel: Boolean)

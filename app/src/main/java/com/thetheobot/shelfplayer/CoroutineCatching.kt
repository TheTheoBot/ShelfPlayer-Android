package com.thetheobot.shelfplayer

import kotlin.coroutines.cancellation.CancellationException

inline fun <T> runCatchingPreservingCancellation(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }
}

suspend inline fun <T> runSuspendCatchingPreservingCancellation(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        Result.failure(throwable)
    }
}

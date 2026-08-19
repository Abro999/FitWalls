package com.fitwalls.app.util

import kotlinx.coroutines.flow.MutableSharedFlow

object PaymentBus {
    val paymentSuccessFlow = MutableSharedFlow<String>()
    val paymentErrorFlow = MutableSharedFlow<Pair<Int, String?>>()

    suspend fun onSuccess(id: String?) {
        id?.let { paymentSuccessFlow.emit(it) }
    }

    suspend fun onError(code: Int, response: String?) {
        paymentErrorFlow.emit(code to response)
    }
}

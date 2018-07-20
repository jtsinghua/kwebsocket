package com.freetsinghua.customerwebsocket.util

import java.io.IOException

/**
 * @author tsinghua
 * @date 2018/7/19
 */
interface WebSocketEventHandler {

    fun onOpen()

    fun onClose()

    fun onError(exception: IOException) {
        println("error: ${exception.message}")
    }

    fun onMessage(message: WebSocketMessage)

    fun onPing() {
        println("ping...")
    }

    fun onPong() {
        println("pong...")
    }
}
package com.freetsinghua.customerwebsocket

import com.freetsinghua.customerwebsocket.util.WebSocket
import com.freetsinghua.customerwebsocket.util.WebSocketEventHandler
import com.freetsinghua.customerwebsocket.util.WebSocketMessage
import java.io.IOException
import java.net.ConnectException
import java.net.URI
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.schedule

/**
 * @author tsinghua
 * @date 2018/7/19
 */
fun main(args: Array<String>) {

    val str = "100001"
    val id = Base64.getEncoder().encodeToString(str.toByteArray(Charset.defaultCharset()))
    val uri = URI("ws://localhost:10086/msg/" + id)
    val webSocket = WebSocket(uri)


    webSocket.setWebSocketEventHandler(object : WebSocketEventHandler {
        override fun onOpen() {
            println("连接成功!@")
            Timer("websocket_client").schedule(10 * 60 * 1000, 130 * 1000) {
                webSocket.sendPong()
            }
        }

        override fun onClose() {
            println("close,,,")
        }

        override fun onMessage(message: WebSocketMessage) {
            println(message.getText())
        }

        override fun onPing() {

        }

        override fun onPong() {

        }

        override fun onError(exception: IOException) {
            //若是连接异常，则重新连接
            println("error: ${exception.message}")
            if (exception is ConnectException) {
                println("重新连接")
                webSocket.reConnect()
            }
        }
    })

    webSocket.connect()
}
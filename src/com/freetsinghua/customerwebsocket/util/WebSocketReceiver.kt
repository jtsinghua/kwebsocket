package com.freetsinghua.customerwebsocket.util

import com.freetsinghua.customerwebsocket.common.CommonConst
import org.slf4j.LoggerFactory
import java.io.DataInputStream
import java.nio.charset.Charset
import kotlin.experimental.and

/**
 * @author tsinghua
 * @date 2018/7/19
 */
class WebSocketReceiver : Thread {
    private var webSocket: WebSocket
    private var input: DataInputStream
    private var eventHandler: WebSocketEventHandler
    private var stop = false
    private val log = LoggerFactory.getLogger(WebSocketReceiver::class.java)

    constructor(webSocket: WebSocket, input: DataInputStream, eventHandler: WebSocketEventHandler) {
        this.webSocket = webSocket
        this.input = input
        this.eventHandler = eventHandler
    }

    override fun run() {
        while (!stop) {
            try {
                val b = input.readByte()
                val opCode = b.and(0xf)
                val length = input.readByte()
                var payLoad = 0L

                if (length < 126) {
                    payLoad = length.toLong()
                } else if (length.toInt() == 126) {
                    payLoad = ((0xff.and(input.readByte().toInt())).shl(8).or(0xff.and(input.readByte().toInt()))).toLong()
                } else if (length.toInt() == 127) {
                    payLoad = input.readLong()
                }

                val messageBytes = ByteArray(payLoad.toInt())

                for (i in 0..payLoad - 1) {
                    messageBytes.set(i.toInt(), input.readByte())
                }

                val tmp = messageBytes.toString(Charset.defaultCharset())
                when (opCode) {
                    CommonConst.OPCODE_TEXT_DATA.toByte() -> {
                        val message = WebSocketMessage(messageBytes)
                        eventHandler.onMessage(message)
                    }
                    CommonConst.OPCODE_CLOSED.toByte() -> {
                        webSocket.close()
                    }
                    CommonConst.OPCODE_PING.toByte() -> {
                        log.info(tmp)
                    }
                    CommonConst.OPCODE_PONG.toByte() -> {
                        println(messageBytes.toString(Charset.defaultCharset()))
                        log.info(tmp)
                    }
                    else ->
                        throw IllegalArgumentException("opCode[$opCode] invalid!")
                }

            } catch (e: Exception) {
                handleError()
            }
        }
    }

    fun stopIt() {
        stop = true
    }

    fun isRunning(): Boolean {
        return !stop
    }

    private fun handleError() {
        stopIt()
        webSocket.handleReceiverError()
    }

}
package com.freetsinghua.customerwebsocket.util

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.PrintStream
import java.net.Socket
import java.net.URI
import java.net.UnknownHostException
import java.nio.charset.Charset
import java.security.SecureRandom
import java.util.ArrayList
import javax.net.ssl.SSLSocketFactory
import kotlin.collections.LinkedHashMap
import kotlin.experimental.or
import kotlin.experimental.xor


/**
 * @author tsinghua
 * @date 2018/7/19
 */
class WebSocket {
    private var uri: URI
    private lateinit var eventHandler: WebSocketEventHandler
    private lateinit var socket: Socket
    private lateinit var input: DataInputStream
    private lateinit var output: PrintStream
    private lateinit var receiver: WebSocketReceiver
    private var handShaking: WebSocketHandShaking
    @Volatile
    private var connected = false

    companion object {
        const val Version = 13
    }

    constructor(uri: URI, protocol: String? = null, extraHeader: Map<String, String>? = null) {
        this.uri = uri
        this.handShaking = WebSocketHandShaking(uri, protocol, extraHeader)
    }


    fun setWebSocketEventHandler(eventHandler: WebSocketEventHandler) {
        this.eventHandler = eventHandler
    }

    fun getWebSocketEventHandler(): WebSocketEventHandler {
        return this.eventHandler
    }

    @Throws(WebSocketException::class)
    fun connect() {
        try {
            if (connected) {
                throw WebSocketException("已经连接")
            }

            socket = createSocket()
            input = DataInputStream(socket.getInputStream())
            output = PrintStream(socket.getOutputStream())

            output.write(handShaking.handShaking())

            var handShakingCompleted = false
            val len = 2048
            var byteArray = ArrayList<Byte>(len)
            var pos = 0
            val handShakingLines = ArrayList<String>()

            while (!handShakingCompleted) {
                byteArray.add(input.readByte())
                ++pos

                if (byteArray[pos - 1] == 0x0A.toByte() && byteArray[pos - 2] == 0x0D.toByte()) {
                    val line = String(byteArray.toByteArray(), Charset.defaultCharset())
                    if (line.trim().equals("")) {
                        handShakingCompleted = true
                    } else {
                        handShakingLines.add(line.substring(0, line.length - 2))
                    }

                    byteArray.clear()
                    pos = 0
                }
            }

            handShakingLines.forEach {
                println(it)
            }

            handShaking.verifyServerStatusLine(handShakingLines.get(0))
            handShakingLines.removeAt(0)

            val headers = LinkedHashMap<String, String>()

            handShakingLines.forEach {
                val split = it.split(delimiters = ":", ignoreCase = false, limit = 2)
                headers.put(split[0], split[1])
            }

            handShaking.verifyServerHandshakeHeaders(headers)

            receiver = WebSocketReceiver(this, input, eventHandler)
            receiver.start()
            connected = true
            eventHandler.onOpen()
        } catch (e: WebSocketException) {
            throw e
        } catch (ex: IOException) {
            throw WebSocketException("error when connecting: ${ex.message}")
        }
    }


    fun handleReceiverError() {
        try {
            if (connected) {
                close()
            }
        } catch (wse: WebSocketException) {
            wse.printStackTrace()
        }

    }

    @Synchronized
    @Throws(WebSocketException::class)
    fun close() {
        if (!connected) {
            return
        }

        sendCloseHandshake()

        if (receiver.isRunning()) {
            receiver.stopIt()
        }

        closeStreams()

        eventHandler.onClose()
    }

    @Synchronized
    fun send(data: String) {
        execute(OPCODE_TEXT, data)
    }

    @Synchronized
    fun sendPing() {
        execute(OPCODE_PING, "ping...")
    }

    @Synchronized
    fun sendPong() {
        execute(OPCODE_PONG, "pong...")
    }

    @Throws(WebSocketException::class)
    private fun execute(flag: Byte, data: String) {
        if (!connected) {
            throw WebSocketException("error while sending text data: not connected")
        }

        try {
            this.sendFrame(flag, true, data.toByteArray())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(WebSocketException::class)
    private fun closeStreams() {
        try {
            input.close()
            output.close()
            socket.close()
        } catch (ioe: IOException) {
            throw WebSocketException("error while closing websocket connection: ${ioe.message}")
        }

    }

    @Synchronized
    @Throws(WebSocketException::class, IOException::class)
    private fun sendFrame(opcode: Byte, masking: Boolean, data: ByteArray) {
        var headerLength = 2

        /**
         * 是否掩码处理
         */
        if (masking) {
            headerLength += 4
        }

        val frame = ByteArrayOutputStream(data.size + headerLength)

        /**
         * FIN:1位，用于描述消息是否结束，如果为1则该消息为消息尾部,如果为零则还有后续数据包;
         */
        val fin = 0x80.toByte()
        val startByte = (fin or opcode)

        frame.write(byteArrayOf(startByte))

        var length = data.size
        var length_field = 0

        /**
         * PayloadData的长度：7位，7+16位，7+64位
         * 如果其值在0-125，则是payload的真实长度。
         * 如果值是126，则后面2个字节形成的16位无符号整型数的值是payload的真实长度。注意，网络字节序，需要转换。
         * 如果值是127，则后面8个字节形成的64位无符号整型数的值是payload的真实长度。注意，网络字节序，需要转换。
         * 长度表示遵循一个原则，用最少的字节表示长度（我理解是尽量减少不必要的传输）。
         */
        if (length < 126) {
            if (masking) {
                length = 0x80 or length
            }
            frame.write(byteArrayOf(length.toByte()))
        } else if (length <= 65535) {
            length_field = 126
            if (masking) {
                length_field = 0x80 or length_field
            }
            frame.write(byteArrayOf(length_field.toByte()))

            val lengthBytes = byteArrayOf(length.toByte())

            frame.write(byteArrayOf(lengthBytes[2]))
            frame.write(byteArrayOf(lengthBytes[3]))
        } else {
            length_field = 127

            if (masking) {
                length_field = 0x80 or length_field
            }

            frame.write(byteArrayOf(length_field.toByte()))
            // Since an integer occupies just 4 bytes we fill the 4 leading length bytes with zero
            frame.write(byteArrayOf(0x0, 0x0, 0x0, 0x0))
            frame.write(byteArrayOf(length.toByte()))
        }

        var mask: ByteArray? = null
        if (masking) {
            mask = generateMask()
            frame.write(mask)

            for (i in 0 until data.size) {
                data[i] = data[i] xor mask!![i % 4]
            }
        }

        frame.write(data)
        output.write(frame.toByteArray())
        output.flush()
    }

    private fun generateMask(): ByteArray? {
        val masking = ByteArray(4)
        val random = SecureRandom()
        random.nextBytes(masking)

        return masking
    }

    /**
     * 0x0表示附加数据帧
     * 0x1表示文本数据帧
     * 0x2表示二进制数据帧
     * 0x3-7暂时无定义，为以后的非控制帧保留
     * 0x8表示连接关闭
     * 0x9表示ping
     * 0xA表示pong
     * 0xB-F暂时无定义，为以后的控制帧保留
     */
    val OPCODE_TEXT: Byte = 0x1
    val OPCODE_CLOSE: Byte = 0x8
    val OPCODE_PING: Byte = 0x9
    val OPCODE_PONG: Byte = 0xA

    @Synchronized
    @Throws(WebSocketException::class)
    private fun sendCloseHandshake() {
        if (!connected) {
            throw WebSocketException("error while sending close handshake: not connected")
        }

        println("Sending close")
        if (!connected) {
            throw WebSocketException("error while sending close")
        }

        try {
            this.sendFrame(OPCODE_CLOSE, true, ByteArray(0))
        } catch (e: IOException) {
            e.printStackTrace()
        }

        connected = false
    }

    private fun createSocket(): Socket {

        val scheme = uri.scheme
        val host = uri.host
        var port = uri.port

        val socket: Socket

        if (scheme != null && scheme.equals("ws")) {
            if (port == -1) {
                port = 80
            }

            try {
                socket = Socket(host, port)
            } catch (ex: UnknownHostException) {
                throw WebSocketException("error when create socket: ${ex.message}")
            } catch (ex: IOException) {
                throw WebSocketException("error when create socket: ${ex.message}")
            }
        } else if (scheme != null && scheme.equals("wss")) {
            if (port == -1) {
                port = 443
            }

            try {
                val factory = SSLSocketFactory.getDefault()
                socket = factory.createSocket(host, port)
            } catch (ex: UnknownHostException) {
                throw WebSocketException("error when create socket: ${ex.message}")
            } catch (ex: IOException) {
                throw WebSocketException("error when create socket: ${ex.message}")
            }
        } else {
            throw WebSocketException("unsupported protocol: $scheme")
        }

        return socket
    }


}
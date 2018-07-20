package com.freetsinghua.customerwebsocket.util

import java.net.URI
import java.nio.charset.Charset
import java.util.*
import kotlin.collections.LinkedHashMap


/**
 * @author tsinghua
 * @date 2018/7/19
 */
class WebSocketHandShaking {

    private var uri: URI
    private var protocol: String?
    private var extraHeaders: Map<String, String>?
    private var key: String

    constructor(uri: URI, protocol: String?, extraHeaders: Map<String, String>?) {
        this.uri = uri
        this.protocol = protocol
        this.extraHeaders = extraHeaders
        this.key = getKey()
    }

    /**
     * GET /chat HTTP/1.1
     * Host: server.example.com *
     * Upgrade: websocket *
     * Connection: Upgrade *
     * Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ== *
     * Origin: http://example.com
     * Sec-WebSocket-Protocol: chat, superchat *
     * Sec-WebSocket-Version: 13 *
     */
    fun handShaking(): ByteArray {
        val path = uri.path
        var host = uri.host

        if (uri.port != -1) {
            host += ":${uri.port}"
        }

        val header = LinkedHashMap<String, String>()
        header.put("Host", host)
        header.put("Upgrade", "websocket")
        header.put("Connection", "Upgrade")
        header.put("Sec-WebSocket-Key", this.key)
        header.put("Sec-WebSocket-Version", WebSocket.Version.toString())

        if (this.protocol != null) {
            header.put("Sec-WebSocket-Protocol", this.protocol!!)
        }

        if (this.extraHeaders != null) {
            extraHeaders!!.forEach { key, value ->
                if (!header.containsKey(key)) {
                    header.put(key, value)
                }
            }
        }

        var handShaking = "GET $path HTTP/1.1\r\n"
        handShaking += this.generateHeader(header)
        handShaking += "\r\n"

        return handShaking.toByteArray(Charset.defaultCharset())
    }

    @Throws(WebSocketException::class)
    fun verifyServerStatusLine(statusLine: String) {
        /**
         * HTTP/1.1 404 Not Found
         */
        val statusCode = statusLine.substring(9, 12).toInt()

        if (statusCode == 407) {
            throw WebSocketException("connection failed: proxy authentication not supported")
        } else if (statusCode == 404) {
            throw WebSocketException("connection failed: 404 not found")
        } else if (statusCode != 101) {
            throw WebSocketException("connection failed: unknown status code $statusCode")
        }

    }

    @Throws(WebSocketException::class)
    fun verifyServerHandshakeHeaders(headers: HashMap<String, String>) {
        if (headers["Upgrade"]!!.toLowerCase().trimStart() != "websocket") {
            throw WebSocketException("connection failed: missing header field in server handshake: Upgrade")
        } else if (headers["Connection"]!!.toLowerCase().trimStart() != "upgrade") {
            throw WebSocketException("connection failed: missing header field in server handshake: Connection")
        }
    }

    private fun getKey(): String {
        val key = ByteArray(16)

        for (i in 0..15) {
            key[i] = random(0, 266).toByte()
        }

        return Base64.getEncoder().encodeToString(key)
    }

    private fun generateHeader(headers: Map<String, String>): String {
        var header = String()

        headers.forEach { key, value ->
            header += key + ":" + value + "\r\n"
        }

        return header
    }

    private fun random(min: Int, max: Int): Int {
        return (Math.random() * max + min).toInt()
    }
}
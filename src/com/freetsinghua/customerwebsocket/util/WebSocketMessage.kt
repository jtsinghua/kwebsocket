package com.freetsinghua.customerwebsocket.util

import java.nio.charset.Charset

class WebSocketMessage {
    private var message: ByteArray

    constructor(message: ByteArray) {
        this.message = message
    }

    fun getText(): String {
        return message.toString(Charset.defaultCharset())
    }
}

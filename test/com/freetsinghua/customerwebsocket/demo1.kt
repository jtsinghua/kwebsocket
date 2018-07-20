package com.freetsinghua.customerwebsocket

import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * @author tsinghua
 * @date 2018/7/20
 */
fun main(args: Array<String>) {

    val data = "ping".toByteArray(Charset.defaultCharset())

    println(data.size)

    println(data.toString(Charset.defaultCharset()))

    val byte = ByteBuffer.allocate(data.size)
    byte.put(data)
    byte.position(0)

    println(byte.remaining())

    val array = ByteArray(data.size)

    var i = 0
    while (byte.hasRemaining()) {
        array[i++] = byte.get()
    }

    array.toString(Charset.defaultCharset()).also(::println)
}
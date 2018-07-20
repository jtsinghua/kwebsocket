package com.freetsinghua.customerwebsocket.common

/**
 * @author tsinghua
 * @date 2018/7/19
 */
class CommonConst {
    /*
    0x0表示附加数据帧
    0x1表示文本数据帧
    0x2表示二进制数据帧
    0x3-7暂时无定义，为以后的非控制帧保留
    0x8表示连接关闭
    0x9表示ping
    0xA表示pong
    0xB-F暂时无定义，为以后的控制帧保留
     */
    companion object {
        val OPCODE_TEXT_DATA = 0x1
        val OPCODE_CLOSED = 0x08
        val OPCODE_PING = 0x9
        val OPCODE_PONG = 0xA
    }
}
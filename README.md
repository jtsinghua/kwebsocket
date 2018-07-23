## kwebsocket
使用kotlin实现Websocket协议，可用在客户端。使用方法见下方例子：

    fun main(args: Array<String>) {
    
        val str = "100001"
        val id = Base64.getEncoder().encodeToString(str.toByteArray(Charset.defaultCharset()))
        val uri = URI("ws://localhost:10086/msg/" + id)
        val webSocket = WebSocket(uri)
    
    
        webSocket.setWebSocketEventHandler(object : WebSocketEventHandler {
            override fun onOpen() {
                webSocket.send("")
    
                Timer("websocket_client").schedule(1 * 1000, 30 * 1000) {
                    webSocket.sendPong()
                }
    
                println("connected,,,")
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
        })
    
        webSocket.connect()
        }
    }
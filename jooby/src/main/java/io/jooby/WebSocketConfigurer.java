package io.jooby;

public interface WebSocketConfigurer {

  WebSocketConfigurer onConnect(WebSocket.OnConnect callback);

  WebSocketConfigurer onMessage(WebSocket.OnMessage callback);

  WebSocketConfigurer onError(WebSocket.OnError callback);

  WebSocketConfigurer onClose(WebSocket.OnClose callback);
}

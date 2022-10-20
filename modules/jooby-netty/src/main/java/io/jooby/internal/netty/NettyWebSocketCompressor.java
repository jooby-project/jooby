/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.http.websocketx.extensions.WebSocketServerExtensionHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.DeflateFrameServerExtensionHandshaker;
import io.netty.handler.codec.http.websocketx.extensions.compression.PerMessageDeflateServerExtensionHandshaker;

class NettyWebSocketCompressor extends WebSocketServerExtensionHandler {
  public NettyWebSocketCompressor(int compressionLevel) {
    super(
        new PerMessageDeflateServerExtensionHandshaker(
            compressionLevel,
            ZlibCodecFactory.isSupportingWindowSizeAndMemLevel(),
            PerMessageDeflateServerExtensionHandshaker.MAX_WINDOW_SIZE,
            false,
            false),
        new DeflateFrameServerExtensionHandshaker(compressionLevel));
  }
}

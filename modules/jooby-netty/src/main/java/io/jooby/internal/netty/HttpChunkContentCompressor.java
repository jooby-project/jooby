/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static io.netty.handler.codec.compression.StandardCompressionOptions.gzip;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContentCompressor;

public class HttpChunkContentCompressor extends HttpContentCompressor {
  public HttpChunkContentCompressor(int compressionLevel) {
    super(gzip(compressionLevel, gzip().windowBits(), gzip().memLevel()));
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof ByteBuf buff) {
      // convert ByteBuf to HttpContent to make it work with compression. This is needed as we use
      // the
      // ChunkedWriteHandler to send files when compression is enabled.
      if (buff.isReadable()) {
        // We only encode non empty buffers, as empty buffers can be used for determining when
        // the content has been flushed and it confuses the HttpContentCompressor
        // if we let it go
        msg = new DefaultHttpContent(buff);
      }
    }
    super.write(ctx, msg, promise);
  }
}

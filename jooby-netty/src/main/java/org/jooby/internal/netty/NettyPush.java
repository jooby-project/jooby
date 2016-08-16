package org.jooby.internal.netty;

import java.util.Map;

import org.jooby.spi.NativePushPromise;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.AsciiString;

public class NettyPush implements NativePushPromise {

  private ChannelHandlerContext ctx;
  private Http2ConnectionEncoder encoder;
  private int streamId;
  private String authority;
  private String scheme;

  public NettyPush(final ChannelHandlerContext ctx, final Http2ConnectionEncoder encoder,
      final int streamId, final String authority, final String scheme) {
    this.ctx = ctx;
    this.encoder = encoder;
    this.streamId = streamId;
    this.authority = authority;
    this.scheme = scheme;
  }

  @Override
  public void push(final String method, final String path, final Map<String, String> headers) {
    ctx.channel().eventLoop().execute(() -> {
      AsciiString streamIdHeader = HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text();
      Http2Connection connection = encoder.connection();
      int nextStreamId = connection.local().incrementAndGetNextStreamId();
      Http2Headers h2headers = new DefaultHttp2Headers()
          .path(path)
          .method(method)
          .authority(authority)
          .scheme(scheme);
      headers.forEach(h2headers::set);
      encoder.writePushPromise(ctx, streamId, nextStreamId, h2headers, 0, ctx.newPromise());

      // TODO: Is there another way of handling a push promise?
      DefaultFullHttpRequest pushRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
          HttpMethod.valueOf(method.toUpperCase()), path, Unpooled.EMPTY_BUFFER,
          new DefaultHttpHeaders(false).set(streamIdHeader, nextStreamId),
          EmptyHttpHeaders.INSTANCE);
      ctx.pipeline().fireChannelRead(pushRequest);
      ctx.pipeline().fireChannelReadComplete();
    });
  }

}

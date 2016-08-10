package org.jooby.internal.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

public class NettyHttp2Handler extends ApplicationProtocolNegotiationHandler {

  private NettyInitializer initializer;

  public NettyHttp2Handler(final NettyInitializer initializer) {
    super(ApplicationProtocolNames.HTTP_1_1);
    this.initializer = initializer;
  }

  @Override
  protected void configurePipeline(final ChannelHandlerContext ctx, final String protocol)
      throws Exception {

    if (ApplicationProtocolNames.HTTP_2.equals(protocol)) {
      DefaultHttp2Connection connection = new DefaultHttp2Connection(true);
      InboundHttp2ToHttpAdapter listener = new InboundHttp2ToHttpAdapterBuilder(connection)
          .propagateSettings(false)
          .validateHttpHeaders(false)
          .maxContentLength(initializer.maxContentLength)
          .build();

      ChannelPipeline pipeline = ctx.pipeline();
      HttpToHttp2ConnectionHandler http2handler = new HttpToHttp2ConnectionHandlerBuilder()
          .frameListener(listener)
          .frameLogger(new Http2FrameLogger(LogLevel.DEBUG))
          .connection(connection)
          .build();
      ctx.channel().attr(NettyRequest.HTT2).set(http2handler.encoder());
      pipeline.addLast(http2handler);
      initializer.pipeline(pipeline, false);
    } else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
      initializer.pipeline(ctx.pipeline(), true);
    } else {
      throw new IllegalStateException("Unknown protocol: " + protocol);
    }
  }
}

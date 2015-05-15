package org.jooby.netty.issues;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import org.jooby.MockUnit;
import org.jooby.internal.netty.NettyResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NettyResponse.class, DefaultFullHttpResponse.class, Unpooled.class,
    DefaultHttpHeaders.class })
public class Issue67 {

  @Test
  public void shouldCloseChannelIfHttpKeepAliveIsOff() throws Exception {
    byte[] bytes = "Hello World!".getBytes();
    new MockUnit(ChannelHandlerContext.class)
        .expect(unit -> {
          ByteBuf buff = unit.mock(ByteBuf.class);
          expect(buff.readableBytes()).andReturn(bytes.length);

          unit.mockStatic(Unpooled.class);
          expect(Unpooled.wrappedBuffer(bytes)).andReturn(buff);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(isA(HttpHeaders.class))).andReturn(headers);

          DefaultFullHttpResponse rsp = unit.mockConstructor(DefaultFullHttpResponse.class,
              new Class[]{HttpVersion.class, HttpResponseStatus.class, ByteBuf.class },
              HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buff);
          expect(rsp.headers()).andReturn(headers);

          ChannelFuture rspfuture = unit.mock(ChannelFuture.class);
          expect(rspfuture.addListener(CLOSE)).andReturn(rspfuture);
          expect(rspfuture.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(rspfuture);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(rsp)).andReturn(rspfuture);

        })
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), 8192,
              false).send(bytes);
        });
  }
}

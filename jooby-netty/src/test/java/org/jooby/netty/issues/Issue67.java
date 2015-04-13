package org.jooby.netty.issues;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import static org.easymock.EasyMock.expect;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import org.jooby.MockUnit;
import org.jooby.internal.netty.NettyOutputStream;
import org.jooby.internal.netty.NettyRequest;
import org.jooby.internal.netty.NettyResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NettyOutputStream.class, DefaultHttpResponse.class })
public class Issue67 {

  @Test
  public void shouldCloseChannelIfHttpKeepAliveIsOff() throws Exception {
    byte[] bytes = "Hello World!".getBytes();
    new MockUnit(NettyRequest.class, NettyResponse.class, ChannelHandlerContext.class,
        ByteBuf.class,
        HttpHeaders.class)
        .expect(unit -> {
          ByteBuf byteBuf = unit.get(ByteBuf.class);
          expect(byteBuf.maxWritableBytes()).andReturn(1024);
          expect(byteBuf.writeBytes(bytes, 0, bytes.length)).andReturn(byteBuf);
        })
        .expect(
            unit -> {
              ByteBuf byteBuf = unit.get(ByteBuf.class);
              expect(byteBuf.readableBytes()).andReturn(bytes.length);

              HttpHeaders headers = unit.get(HttpHeaders.class);
              expect(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).andReturn(null);
              expect(headers.remove(HttpHeaders.Names.TRANSFER_ENCODING)).andReturn(headers);
              expect(headers.set(HttpHeaders.Names.CONTENT_LENGTH, "" + bytes.length)).andReturn(
                  headers);

              HttpHeaders rspheaders = unit.get(HttpHeaders.class);
              expect(rspheaders.set(headers)).andReturn(rspheaders);

              DefaultFullHttpResponse rsp = unit.mockConstructor(DefaultFullHttpResponse.class,
                  new Class[]{HttpVersion.class, HttpResponseStatus.class, ByteBuf.class },
                  HttpVersion.HTTP_1_1, HttpResponseStatus.OK, byteBuf);
              expect(rsp.headers()).andReturn(rspheaders);

              ChannelFuture rspfuture = unit.mock(ChannelFuture.class);
              expect(rspfuture.addListener(CLOSE)).andReturn(rspfuture);
              expect(rspfuture.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(rspfuture);

              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.writeAndFlush(rsp)).andReturn(rspfuture);

            })
        .run(unit -> {
          NettyOutputStream stream = new NettyOutputStream(new NettyResponse(unit
              .get(ChannelHandlerContext.class), false), unit
              .get(ChannelHandlerContext.class), unit.get(ByteBuf.class), false, unit
              .get(HttpHeaders.class));
          stream.write(bytes);
          stream.close();
        });
  }
}

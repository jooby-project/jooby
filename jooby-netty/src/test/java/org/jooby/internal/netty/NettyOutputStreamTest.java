package org.jooby.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;

import org.jooby.MockUnit;
import org.junit.Test;

public class NettyOutputStreamTest {

  @Test
  public void defaults() throws Exception {
    boolean keepAlive = true;
    new MockUnit(NettyResponse.class, ChannelHandlerContext.class, ByteBuf.class, HttpHeaders.class)
        .run(unit -> {
          new NettyOutputStream(
              unit.get(NettyResponse.class),
              unit.get(ChannelHandlerContext.class),
              unit.get(ByteBuf.class),
              keepAlive, unit.get(HttpHeaders.class)
          );
        });
  }
}

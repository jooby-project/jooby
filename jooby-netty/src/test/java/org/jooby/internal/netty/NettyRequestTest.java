package org.jooby.internal.netty;

import static org.easymock.EasyMock.expect;

import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.Attribute;

public class NettyRequestTest {

  private Block channel = unit -> {
    Channel channel = unit.mock(Channel.class);
    unit.registerMock(Channel.class, channel);

    ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
    expect(ctx.channel()).andReturn(channel);
  };

  @SuppressWarnings("unchecked")
  private Block newObject = unit -> {
    Attribute<Boolean> attr = unit.mock(Attribute.class);
    attr.set(false);

    Channel channel = unit.get(Channel.class);
    unit.get(ChannelHandlerContext.class);
    expect(channel.attr(NettyRequest.ASYNC)).andReturn(attr);

    HttpRequest req = unit.get(HttpRequest.class);
    expect(req.uri()).andReturn("/");
  };

  @Test
  public void newObject() throws Exception {
    new MockUnit(ChannelHandlerContext.class, HttpRequest.class)
        .expect(channel)
        .expect(newObject)
        .run(unit -> {
          new NettyRequest(unit.get(ChannelHandlerContext.class), unit.get(HttpRequest.class),
              "target", 60);
        });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unknownUpgrade() throws Exception {
    new MockUnit(ChannelHandlerContext.class, HttpRequest.class)
        .expect(channel)
        .expect(newObject)
        .run(unit -> {
          new NettyRequest(unit.get(ChannelHandlerContext.class), unit.get(HttpRequest.class),
              "target", 60)
                  .upgrade(Object.class);
        });
  }
}

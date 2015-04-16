package org.jooby.internal.netty;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

import org.jooby.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NettyOutputStream.class, DefaultFullHttpResponse.class })
public class NettyOutputStreamTest {

  MockUnit.Block flush0KeepAlive = unit -> {
    expect(unit.get(NettyResponse.class).status()).andReturn(HttpResponseStatus.OK);

    HttpHeaders rspheaders = unit.mock(HttpHeaders.class);
    expect(rspheaders.set(unit.get(HttpHeaders.class))).andReturn(rspheaders);

    DefaultFullHttpResponse rsp = unit.mockConstructor(
        DefaultFullHttpResponse.class,
        new Class[]{HttpVersion.class, HttpResponseStatus.class, ByteBuf.class },
        HttpVersion.HTTP_1_1, HttpResponseStatus.OK, unit.get(ByteBuf.class)
        );

    expect(rsp.headers()).andReturn(rspheaders);

    ChannelFuture future = unit.mock(ChannelFuture.class);
    expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future);

    ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
    expect(ctx.writeAndFlush(rsp)).andReturn(future);
  };

  MockUnit.Block flush1KeepAlive = unit -> {
    expect(unit.get(NettyResponse.class).status()).andReturn(HttpResponseStatus.OK);

    HttpHeaders rspheaders = unit.mock(HttpHeaders.class);
    expect(rspheaders.set(unit.get(HttpHeaders.class))).andReturn(rspheaders);

    DefaultHttpResponse rsp = unit.mockConstructor(
        DefaultHttpResponse.class,
        new Class[]{HttpVersion.class, HttpResponseStatus.class },
        HttpVersion.HTTP_1_1, HttpResponseStatus.OK
        );

    expect(rsp.headers()).andReturn(rspheaders);

    ChannelFuture future = unit.mock(ChannelFuture.class);
    expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future);

    ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
    expect(ctx.write(rsp)).andReturn(future);
  };

  MockUnit.Block flushChunkKeepAlive = unit -> {
    expect(unit.get(NettyResponse.class).status()).andReturn(HttpResponseStatus.OK);

    HttpHeaders rspheaders = unit.mock(HttpHeaders.class);
    expect(rspheaders.set(unit.get(HttpHeaders.class))).andReturn(rspheaders);

    DefaultHttpResponse rsp = unit.mockConstructor(
        DefaultHttpResponse.class,
        new Class[]{HttpVersion.class, HttpResponseStatus.class },
        HttpVersion.HTTP_1_1, HttpResponseStatus.OK
        );

    expect(rsp.headers()).andReturn(rspheaders);

    ChannelFuture future = unit.mock(ChannelFuture.class);
    expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future).times(2);

    ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
    expect(ctx.write(rsp)).andReturn(future);
    expect(ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)).andReturn(future);
  };

  MockUnit.Block flushChunkNoKeepAlive = unit -> {
    expect(unit.get(NettyResponse.class).status()).andReturn(HttpResponseStatus.OK);

    HttpHeaders rspheaders = unit.mock(HttpHeaders.class);
    expect(rspheaders.set(unit.get(HttpHeaders.class))).andReturn(rspheaders);

    DefaultHttpResponse rsp = unit.mockConstructor(
        DefaultHttpResponse.class,
        new Class[]{HttpVersion.class, HttpResponseStatus.class },
        HttpVersion.HTTP_1_1, HttpResponseStatus.OK
        );

    expect(rsp.headers()).andReturn(rspheaders);

    ChannelFuture future = unit.mock(ChannelFuture.class);
    expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future).times(2);
    expect(future.addListener(CLOSE)).andReturn(future);

    ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
    expect(ctx.write(rsp)).andReturn(future);
    expect(ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)).andReturn(future);
  };

  MockUnit.Block flush0NoKeepAlive = unit -> {
    expect(unit.get(NettyResponse.class).status()).andReturn(HttpResponseStatus.OK);

    HttpHeaders rspheaders = unit.mock(HttpHeaders.class);
    expect(rspheaders.set(unit.get(HttpHeaders.class))).andReturn(rspheaders);

    DefaultFullHttpResponse rsp = unit.mockConstructor(
        DefaultFullHttpResponse.class,
        new Class[]{HttpVersion.class, HttpResponseStatus.class, ByteBuf.class },
        HttpVersion.HTTP_1_1, HttpResponseStatus.OK, unit.get(ByteBuf.class)
        );

    expect(rsp.headers()).andReturn(rspheaders);

    ChannelFuture future = unit.mock(ChannelFuture.class);
    expect(future.addListener(CLOSE)).andReturn(future);
    expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future);

    ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
    expect(ctx.writeAndFlush(rsp)).andReturn(future);
  };

  MockUnit.Block chunk = unit -> {
    ByteBuf byteBuf = unit.get(ByteBuf.class);
    expect(byteBuf.clear()).andReturn(byteBuf);

    ByteBuf copy = unit.mock(ByteBuf.class);
    expect(byteBuf.copy()).andReturn(copy);

    DefaultHttpContent rsp = unit.mockConstructor(
        DefaultHttpContent.class,
        new Class[]{ByteBuf.class },
        copy
        );

    ChannelFuture future = unit.mock(ChannelFuture.class);
    expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future);

    ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
    expect(ctx.writeAndFlush(rsp)).andReturn(future);
  };

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

  @Test
  public void writeOneByte() throws Exception {
    boolean keepAlive = true;
    new MockUnit(NettyResponse.class, ChannelHandlerContext.class, ByteBuf.class, HttpHeaders.class)
        .expect(unit -> {
          ByteBuf byteBuf = unit.get(ByteBuf.class);
          expect(byteBuf.maxWritableBytes()).andReturn(1024);
          expect(byteBuf.writeByte('c')).andReturn(byteBuf);
        })
        .run(unit -> {
          new NettyOutputStream(
              unit.get(NettyResponse.class),
              unit.get(ChannelHandlerContext.class),
              unit.get(ByteBuf.class),
              keepAlive, unit.get(HttpHeaders.class)
            ).write('c');
          });
  }

  @Test
  public void writeOneByteWithOverflow() throws Exception {
    boolean keepAlive = true;
    new MockUnit(NettyResponse.class, ChannelHandlerContext.class, ByteBuf.class, HttpHeaders.class)
        .expect(unit -> {
          ByteBuf byteBuf = unit.get(ByteBuf.class);
          expect(byteBuf.maxWritableBytes()).andReturn(0);
          expect(byteBuf.writeByte('c')).andReturn(byteBuf);
        })
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);

          expect(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).andReturn("1");

          expect(headers.set("Connection", "keep-alive")).andReturn(headers);
        })
        .expect(flush0KeepAlive)
        .run(unit -> {
          new NettyOutputStream(
              unit.get(NettyResponse.class),
              unit.get(ChannelHandlerContext.class),
              unit.get(ByteBuf.class),
              keepAlive, unit.get(HttpHeaders.class)
            ).write('c');
          });
  }

  @Test
  public void writeBytes() throws Exception {
    boolean keepAlive = true;
    new MockUnit(NettyResponse.class, ChannelHandlerContext.class, ByteBuf.class, HttpHeaders.class)
        .expect(unit -> {
          ByteBuf byteBuf = unit.get(ByteBuf.class);
          expect(byteBuf.maxWritableBytes()).andReturn(1024);
          expect(byteBuf.writeBytes(aryEq(new byte[]{'a', 'b', 'c' }), eq(0), eq(3)))
              .andReturn(byteBuf);
        })
        .run(unit -> {
          new NettyOutputStream(
              unit.get(NettyResponse.class),
              unit.get(ChannelHandlerContext.class),
              unit.get(ByteBuf.class),
              keepAlive, unit.get(HttpHeaders.class)
            ).write(new byte[]{'a', 'b', 'c' });
          });
  }

  @Test
  public void writeBytesOverflow() throws Exception {
    boolean keepAlive = true;
    new MockUnit(NettyResponse.class, ChannelHandlerContext.class, ByteBuf.class, HttpHeaders.class)
        .expect(unit -> {
          ByteBuf byteBuf = unit.get(ByteBuf.class);
          expect(byteBuf.maxWritableBytes()).andReturn(1);
          expect(byteBuf.writeBytes(aryEq(new byte[]{'a', 'b', 'c' }), eq(0), eq(1)))
              .andReturn(byteBuf);
          expect(byteBuf.maxWritableBytes()).andReturn(3);
          expect(byteBuf.writeBytes(aryEq(new byte[]{'a', 'b', 'c' }), eq(1), eq(2)))
              .andReturn(byteBuf);
        })
        .expect(flush1KeepAlive)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);

          expect(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).andReturn("3");

          expect(headers.set("Connection", "keep-alive")).andReturn(headers);
        })
        .expect(chunk)
        .run(unit -> {
          new NettyOutputStream(
              unit.get(NettyResponse.class),
              unit.get(ChannelHandlerContext.class),
              unit.get(ByteBuf.class),
              keepAlive, unit.get(HttpHeaders.class)
            ).write(new byte[]{'a', 'b', 'c' });
          });
  }

  @Test
  public void writeZeroBytes() throws Exception {
    boolean keepAlive = true;
    new MockUnit(NettyResponse.class, ChannelHandlerContext.class, ByteBuf.class, HttpHeaders.class)
        .expect(unit -> {
          ByteBuf byteBuf = unit.get(ByteBuf.class);
          expect(byteBuf.maxWritableBytes()).andReturn(1);
        })
        .run(unit -> {
          new NettyOutputStream(
              unit.get(NettyResponse.class),
              unit.get(ChannelHandlerContext.class),
              unit.get(ByteBuf.class),
              keepAlive, unit.get(HttpHeaders.class)
            ).write(new byte[0]);
          });
  }

  @Test
  public void reset() throws Exception {
    boolean keepAlive = true;
    new MockUnit(NettyResponse.class, ChannelHandlerContext.class, ByteBuf.class, HttpHeaders.class)
        .expect(unit -> {
          ByteBuf byteBuf = unit.get(ByteBuf.class);
          expect(byteBuf.clear()).andReturn(byteBuf);
          expect(byteBuf.maxWritableBytes()).andReturn(2);
          expect(byteBuf.writeBytes(aryEq(new byte[]{'a', 'b', 'c' }), eq(0), eq(2)))
              .andReturn(byteBuf);
          expect(byteBuf.maxWritableBytes()).andReturn(2);
          expect(byteBuf.writeBytes(aryEq(new byte[]{'a', 'b', 'c' }), eq(2), eq(1)))
              .andReturn(byteBuf);
        })
        .expect(flush1KeepAlive)
        .expect(chunk)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);

          expect(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).andReturn("3");

          expect(headers.set("Connection", "keep-alive")).andReturn(headers);
        })
        .run(unit -> {
          NettyOutputStream out = new NettyOutputStream(
              unit.get(NettyResponse.class),
              unit.get(ChannelHandlerContext.class),
              unit.get(ByteBuf.class),
              keepAlive, unit.get(HttpHeaders.class)
              );
          out.write(new byte[]{'a', 'b', 'c' });
          assertEquals(1, out.chunkState);
          out.reset();
          assertEquals(0, out.chunkState);
        });
  }

  @Test
  public void committed() throws Exception {
    boolean keepAlive = true;
    new MockUnit(NettyResponse.class, ChannelHandlerContext.class, ByteBuf.class, HttpHeaders.class)
        .expect(unit -> {
          ByteBuf byteBuf = unit.get(ByteBuf.class);
          expect(byteBuf.maxWritableBytes()).andReturn(2);
          expect(byteBuf.writeBytes(aryEq(new byte[]{'a', 'b', 'c' }), eq(0), eq(2)))
              .andReturn(byteBuf);
          expect(byteBuf.maxWritableBytes()).andReturn(2);
          expect(byteBuf.writeBytes(aryEq(new byte[]{'a', 'b', 'c' }), eq(2), eq(1)))
              .andReturn(byteBuf);
        })
        .expect(flush1KeepAlive)
        .expect(chunk)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);

          expect(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).andReturn("3");

          expect(headers.set("Connection", "keep-alive")).andReturn(headers);
        })
        .run(unit -> {
          NettyOutputStream out = new NettyOutputStream(
              unit.get(NettyResponse.class),
              unit.get(ChannelHandlerContext.class),
              unit.get(ByteBuf.class),
              keepAlive, unit.get(HttpHeaders.class)
              );
          out.write(new byte[]{'a', 'b', 'c' });
          assertEquals(true, out.committed());
        });
  }

  @Test
  public void closeKeepAlive() throws Exception {
    boolean keepAlive = true;
    new MockUnit(NettyResponse.class, ChannelHandlerContext.class, ByteBuf.class, HttpHeaders.class)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);

          expect(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).andReturn("0");

          expect(headers.set("Connection", "keep-alive")).andReturn(headers);
        })
        .expect(flush0KeepAlive)
        .run(unit -> {
          new NettyOutputStream(
              unit.get(NettyResponse.class),
              unit.get(ChannelHandlerContext.class),
              unit.get(ByteBuf.class),
              keepAlive, unit.get(HttpHeaders.class)
            ).close();
          });
  }

  @Test
  public void closeChunkKeepAlive() throws Exception {
    boolean keepAlive = true;
    new MockUnit(NettyResponse.class, ChannelHandlerContext.class, ByteBuf.class, HttpHeaders.class)
        .expect(unit -> {
          ByteBuf byteBuf = unit.get(ByteBuf.class);
          expect(byteBuf.maxWritableBytes()).andReturn(2);
          expect(byteBuf.writeBytes(aryEq(new byte[]{'a', 'b', 'c' }), eq(0), eq(2)))
              .andReturn(byteBuf);
          expect(byteBuf.maxWritableBytes()).andReturn(2);
          expect(byteBuf.writeBytes(aryEq(new byte[]{'a', 'b', 'c' }), eq(2), eq(1)))
              .andReturn(byteBuf);
        })
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);

          expect(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).andReturn("3");
          expect(headers.contains(HttpHeaders.Names.CONTENT_LENGTH)).andReturn(true);

          expect(headers.set("Connection", "keep-alive")).andReturn(headers);
        })
        .expect(flushChunkKeepAlive)
        .expect(chunk)
        .expect(chunk)
        .run(unit -> {
          NettyOutputStream out = new NettyOutputStream(
              unit.get(NettyResponse.class),
              unit.get(ChannelHandlerContext.class),
              unit.get(ByteBuf.class),
              keepAlive, unit.get(HttpHeaders.class)
              );
          out.write(new byte[]{'a', 'b', 'c' });
          out.close();
        });
  }

  @Test
  public void closeChunkNoKeepAlive() throws Exception {
    boolean keepAlive = false;
    new MockUnit(NettyResponse.class, ChannelHandlerContext.class, ByteBuf.class, HttpHeaders.class)
        .expect(unit -> {
          ByteBuf byteBuf = unit.get(ByteBuf.class);
          expect(byteBuf.maxWritableBytes()).andReturn(2);
          expect(byteBuf.writeBytes(aryEq(new byte[]{'a', 'b', 'c' }), eq(0), eq(2)))
              .andReturn(byteBuf);
          expect(byteBuf.maxWritableBytes()).andReturn(2);
          expect(byteBuf.writeBytes(aryEq(new byte[]{'a', 'b', 'c' }), eq(2), eq(1)))
              .andReturn(byteBuf);
        })
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);

          expect(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).andReturn("3");
          expect(headers.contains(HttpHeaders.Names.CONTENT_LENGTH)).andReturn(true);
        })
        .expect(flushChunkNoKeepAlive)
        .expect(chunk)
        .expect(chunk)
        .run(unit -> {
          NettyOutputStream out = new NettyOutputStream(
              unit.get(NettyResponse.class),
              unit.get(ChannelHandlerContext.class),
              unit.get(ByteBuf.class),
              keepAlive, unit.get(HttpHeaders.class)
              );
          out.write(new byte[]{'a', 'b', 'c' });
          out.close();
        });
  }

  @Test
  public void closeChunkNoLenNoKeepAlive() throws Exception {
    boolean keepAlive = true;
    new MockUnit(NettyResponse.class, ChannelHandlerContext.class, ByteBuf.class, HttpHeaders.class)
        .expect(unit -> {
          ByteBuf byteBuf = unit.get(ByteBuf.class);
          expect(byteBuf.maxWritableBytes()).andReturn(2);
          expect(byteBuf.writeBytes(aryEq(new byte[]{'a', 'b', 'c' }), eq(0), eq(2)))
              .andReturn(byteBuf);
          expect(byteBuf.maxWritableBytes()).andReturn(2);
          expect(byteBuf.writeBytes(aryEq(new byte[]{'a', 'b', 'c' }), eq(2), eq(1)))
              .andReturn(byteBuf);
        })
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);

          expect(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).andReturn(null);
          expect(headers.set(HttpHeaders.Names.TRANSFER_ENCODING, "chunked")).andReturn(null);
          expect(headers.contains(HttpHeaders.Names.CONTENT_LENGTH)).andReturn(false);
        })
        .expect(flushChunkNoKeepAlive)
        .expect(chunk)
        .expect(chunk)
        .run(unit -> {
          NettyOutputStream out = new NettyOutputStream(
              unit.get(NettyResponse.class),
              unit.get(ChannelHandlerContext.class),
              unit.get(ByteBuf.class),
              keepAlive, unit.get(HttpHeaders.class)
              );
          out.write(new byte[]{'a', 'b', 'c' });
          out.close();
        });
  }

  @Test
  public void closeNoKeepAlive() throws Exception {
    boolean keepAlive = false;
    new MockUnit(NettyResponse.class, ChannelHandlerContext.class, ByteBuf.class, HttpHeaders.class)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);

          expect(headers.get(HttpHeaders.Names.CONTENT_LENGTH)).andReturn("0");

        })
        .expect(flush0NoKeepAlive)
        .run(unit -> {
          new NettyOutputStream(
              unit.get(NettyResponse.class),
              unit.get(ChannelHandlerContext.class),
              unit.get(ByteBuf.class),
              keepAlive, unit.get(HttpHeaders.class)
            ).close();
          });
  }

}

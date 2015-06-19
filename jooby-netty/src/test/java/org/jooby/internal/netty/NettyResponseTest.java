package org.jooby.internal.netty;

import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.Attribute;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jooby.MockUnit;
import org.jooby.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NettyResponse.class, DefaultFullHttpResponse.class, Unpooled.class,
    DefaultHttpHeaders.class, DefaultHttpResponse.class, ChunkedStream.class,
    DefaultFileRegion.class })
public class NettyResponseTest {

  byte[] bytes = "bytes".getBytes();
  ByteBuffer buffer = ByteBuffer.wrap(bytes);
  int bufferSize = 8192;

  private Block wrapBytes = unit -> {
    ByteBuf buffer = unit.get(ByteBuf.class);

    unit.mockStatic(Unpooled.class);
    expect(Unpooled.wrappedBuffer(bytes)).andReturn(buffer);
  };

  private Block wrapBuffer = unit -> {
    ByteBuf buf = unit.get(ByteBuf.class);

    unit.mockStatic(Unpooled.class);
    expect(Unpooled.wrappedBuffer(buffer)).andReturn(buf);
  };

  private Block headers = unit -> {
    DefaultHttpHeaders headers = unit.mockConstructor(DefaultHttpHeaders.class);

    unit.registerMock(DefaultHttpHeaders.class, headers);
  };

  private Block deflen = unit -> {
    DefaultHttpHeaders headers = unit.get(DefaultHttpHeaders.class);

    ByteBuf buffer = unit.get(ByteBuf.class);
    expect(buffer.readableBytes()).andReturn(bytes.length);

    expect(headers.contains(HttpHeaders.Names.CONTENT_LENGTH)).andReturn(false);
    expect(headers.remove(HttpHeaders.Names.TRANSFER_ENCODING)).andReturn(headers);
    expect(headers.set(HttpHeaders.Names.CONTENT_LENGTH, bytes.length)).andReturn(headers);
  };

  private Block connkeep = unit -> {
    DefaultHttpHeaders headers = unit.get(DefaultHttpHeaders.class);

    expect(headers.set(HttpHeaders.Names.CONNECTION, "keep-alive")).andReturn(headers);
  };

  private Block len = unit -> {
    DefaultHttpHeaders headers = unit.get(DefaultHttpHeaders.class);

    expect(headers.contains(HttpHeaders.Names.CONTENT_LENGTH)).andReturn(true);
  };

  private Block fullResponse = unit -> {
    DefaultFullHttpResponse rsp = unit.mockConstructor(
        DefaultFullHttpResponse.class,
        new Class[]{HttpVersion.class, HttpResponseStatus.class, ByteBuf.class },
        HttpVersion.HTTP_1_1, HttpResponseStatus.OK, unit.get(ByteBuf.class)
        );

    HttpHeaders headers = unit.mock(HttpHeaders.class);
    expect(headers.set(unit.get(DefaultHttpHeaders.class))).andReturn(headers);

    expect(rsp.headers()).andReturn(headers);

    unit.registerMock(HttpResponse.class, rsp);
  };

  private Block keeAliveWithLen = unit -> {
    DefaultHttpHeaders headers = unit.get(DefaultHttpHeaders.class);

    expect(headers.contains(HttpHeaders.Names.CONTENT_LENGTH)).andReturn(true);

    ChannelFuture future = unit.get(ChannelFuture.class);

    expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future);
  };

  private Block fireExceptionOnFailure = unit -> {
    ChannelFuture future = unit.get(ChannelFuture.class);

    expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future);
  };

  private Block noKeepAlive = unit -> {
    DefaultHttpHeaders headers = unit.get(DefaultHttpHeaders.class);

    expect(headers.contains(HttpHeaders.Names.CONTENT_LENGTH)).andReturn(true);

    ChannelFuture future = unit.get(ChannelFuture.class);

    expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future);
    expect(future.addListener(CLOSE)).andReturn(future);
  };

  private Block noKeepAliveNoLen = unit -> {
    DefaultHttpHeaders headers = unit.get(DefaultHttpHeaders.class);

    expect(headers.contains(HttpHeaders.Names.CONTENT_LENGTH)).andReturn(false);

    ChannelFuture future = unit.get(ChannelFuture.class);

    expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future);
    expect(future.addListener(CLOSE)).andReturn(future);
  };

  @Test
  public void defaults() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive);
        });
  }

  @Test
  public void headers() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    List<String> v = Arrays.asList("h1");
    new MockUnit(ChannelHandlerContext.class)
        .expect(unit -> {
          DefaultHttpHeaders headers = unit.mockConstructor(DefaultHttpHeaders.class);
          expect(headers.getAll("h")).andReturn(v);
        })
        .run(unit -> {
          assertEquals(v, new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize,
              keepAlive)
              .headers("h"));
        });
  }

  @Test
  public void noHeaders() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    List<String> v = Collections.emptyList();
    new MockUnit(ChannelHandlerContext.class)
        .expect(unit -> {
          DefaultHttpHeaders headers = unit.mockConstructor(DefaultHttpHeaders.class);
          expect(headers.getAll("h")).andReturn(null);
        })
        .run(unit -> {
          assertEquals(v, new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize,
              keepAlive)
              .headers("h"));
        });
  }

  @Test
  public void header() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class)
        .expect(unit -> {
          DefaultHttpHeaders headers = unit.mockConstructor(DefaultHttpHeaders.class);
          expect(headers.get("h")).andReturn("v");
        })
        .run(unit -> {
          assertEquals(Optional.of("v"),
              new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive)
                  .header("h")
            );
          });
  }

  @Test
  public void noHeader() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class)
        .expect(unit -> {
          DefaultHttpHeaders headers = unit.mockConstructor(DefaultHttpHeaders.class);
          expect(headers.get("h")).andReturn(null);
        })
        .run(unit -> {
          assertEquals(Optional.empty(),
              new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive)
                  .header("h")
            );
          });
  }

  @Test
  public void setHeader() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class)
        .expect(unit -> {
          DefaultHttpHeaders headers = unit.mockConstructor(DefaultHttpHeaders.class);
          expect(headers.set("h", "v")).andReturn(headers);
        })
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive)
              .header("h", "v");
        });
  }

  @Test
  public void setHeaders() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class)
        .expect(unit -> {
          DefaultHttpHeaders headers = unit.mockConstructor(DefaultHttpHeaders.class);
          expect(headers.remove("h")).andReturn(headers);
          expect(headers.add("h", Arrays.asList("v1", "v2"))).andReturn(headers);
        })
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive)
              .header("h", Arrays.asList("v1", "v2"));
        });
  }

  @Test
  public void sendBytesSetDefLenAndKeepAlive() throws Exception {
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class)
        .expect(wrapBytes)
        .expect(headers)
        .expect(deflen)
        .expect(connkeep)
        .expect(fullResponse)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(unit.get(HttpResponse.class))).andReturn(future);
        })
        .expect(keeAliveWithLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive)
              .send(bytes);
        });
  }

  @Test
  public void sendBufferSetDefLenAndKeepAlive() throws Exception {
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class)
        .expect(wrapBuffer)
        .expect(headers)
        .expect(deflen)
        .expect(connkeep)
        .expect(fullResponse)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(unit.get(HttpResponse.class))).andReturn(future);
        })
        .expect(keeAliveWithLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive)
              .send(buffer);
        });
  }

  @Test
  public void sendBytesKeepAlive() throws Exception {
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class)
        .expect(wrapBytes)
        .expect(headers)
        .expect(len)
        .expect(connkeep)
        .expect(fullResponse)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(unit.get(HttpResponse.class))).andReturn(future);
        })
        .expect(keeAliveWithLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive)
              .send(bytes);
        });
  }

  @Test
  public void sendBytesNoKeepAlive() throws Exception {
    boolean keepAlive = false;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class)
        .expect(wrapBytes)
        .expect(headers)
        .expect(len)
        .expect(fullResponse)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(unit.get(HttpResponse.class))).andReturn(future);
        })
        .expect(noKeepAlive)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive)
              .send(bytes);
        });
  }

  @Test
  public void sendBytesKeepAliveOffNoLen() throws Exception {
    boolean keepAlive = false;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class)
        .expect(wrapBytes)
        .expect(headers)
        .expect(len)
        .expect(fullResponse)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(unit.get(HttpResponse.class))).andReturn(future);
        })
        .expect(noKeepAliveNoLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive)
              .send(bytes);
        });
  }

  @Test
  public void sendEmptyStream() throws Exception {
    boolean keepAlive = false;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, InputStream.class)
        .expect(unit -> {
          InputStream stream = unit.get(InputStream.class);
          expect(stream.read(unit.capture(byte[].class), eq(0), eq(bufferSize))).andReturn(-1);
        })
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive)
              .send(unit.get(InputStream.class));
        }, unit -> {
          assertEquals(bufferSize, unit.captured(byte[].class).iterator().next().length);
        });
  }

  @Test
  public void sendHeadChunk() throws Exception {
    boolean keepAlive = false;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, InputStream.class)
        .expect(
            unit -> {
              InputStream stream = unit.get(InputStream.class);
              expect(stream.read(unit.capture(byte[].class), eq(0), eq(bytes.length))).andReturn(
                  bytes.length / 2);
              expect(stream.read(unit.capture(byte[].class), eq(2), eq(3))).andReturn(-1);
            })
        .expect(unit -> {
          ByteBuf buffer = unit.get(ByteBuf.class);

          unit.mockStatic(Unpooled.class);
          expect(Unpooled.wrappedBuffer(aryEq(new byte[bytes.length]), eq(0),
              eq(bytes.length / 2))).andReturn(buffer);
        })
        .expect(headers)
        .expect(len)
        .expect(fullResponse)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(unit.get(HttpResponse.class))).andReturn(future);
        })
        .expect(noKeepAliveNoLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), bytes.length, keepAlive)
              .send(unit.get(InputStream.class));
        });
  }

  @Test
  public void sendChunks() throws Exception {
    boolean keepAlive = true;
    int bufferSize = 10;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, InputStream.class)
        .expect(unit -> {
          InputStream stream = unit.get(InputStream.class);
          expect(stream.read(unit.capture(byte[].class), eq(0), eq(bufferSize))).andReturn(
              bufferSize);
        })
        .expect(unit -> {
          ByteBuf buffer = unit.get(ByteBuf.class);

          unit.mockStatic(Unpooled.class);
          expect(Unpooled.wrappedBuffer(aryEq(new byte[bufferSize]), eq(0), eq(bufferSize)))
              .andReturn(buffer);
        })
        .expect(headers)
        .expect(unit -> {
          DefaultHttpHeaders headers = unit.get(DefaultHttpHeaders.class);
          expect(headers.contains(HttpHeaders.Names.CONTENT_LENGTH)).andReturn(false);
          expect(headers.set(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED))
              .andReturn(headers);
        })
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultHttpResponse.class,
              new Class[]{HttpVersion.class,
                  HttpResponseStatus.class }, HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(unit.get(DefaultHttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.write(unit.get(HttpResponse.class))).andReturn(future);
        })
        .expect(fireExceptionOnFailure)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.write(unit.get(ByteBuf.class))).andReturn(future);
        })
        .expect(fireExceptionOnFailure)
        .expect(
            unit -> {
              ChunkedStream chunkedStream = unit.mockConstructor(ChunkedStream.class, new Class[]{
                  InputStream.class, int.class }, unit.get(InputStream.class), bufferSize);
              ChannelFuture future = unit.get(ChannelFuture.class);

              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.write(chunkedStream)).andReturn(future);
            })
        .expect(fireExceptionOnFailure)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)).andReturn(future);
        })
        .expect(noKeepAliveNoLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive)
              .send(unit.get(InputStream.class));
        });
  }

  @Test
  public void sendFileChannel() throws Exception {
    boolean keepAlive = false;
    FileChannel channel = newFileChannel(8192);
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class)
        .expect(headers)
        .expect(unit -> {
          DefaultHttpHeaders headers = unit.get(DefaultHttpHeaders.class);
          expect(headers.contains(HttpHeaders.Names.CONTENT_LENGTH)).andReturn(false);
          expect(headers.remove(HttpHeaders.Names.TRANSFER_ENCODING)).andReturn(headers);
          expect(headers.set(HttpHeaders.Names.CONTENT_LENGTH, 8192L)).andReturn(headers);
        })
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultHttpResponse.class,
              new Class[]{HttpVersion.class,
                  HttpResponseStatus.class }, HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(unit.get(DefaultHttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.write(unit.get(HttpResponse.class))).andReturn(future);
        })
        .expect(fireExceptionOnFailure)
        .expect(
            unit -> {
              DefaultFileRegion region = unit.mockConstructor(DefaultFileRegion.class,
                  new Class[]{FileChannel.class, long.class, long.class }, channel, 0L,
                  channel.size());

              unit.registerMock(DefaultFileRegion.class, region);
            })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.write(unit.get(DefaultFileRegion.class))).andReturn(future);
        })
        .expect(fireExceptionOnFailure)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)).andReturn(future);
        })
        .expect(noKeepAliveNoLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive)
              .send(channel);
        });
  }

  @Test
  public void sendFileChannelNoLen() throws Exception {
    boolean keepAlive = false;
    FileChannel channel = newFileChannel(8192);
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class)
        .expect(headers)
        .expect(unit -> {
          DefaultHttpHeaders headers = unit.get(DefaultHttpHeaders.class);
          expect(headers.contains(HttpHeaders.Names.CONTENT_LENGTH)).andReturn(true);
        })
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultHttpResponse.class,
              new Class[]{HttpVersion.class,
                  HttpResponseStatus.class }, HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(unit.get(DefaultHttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.write(unit.get(HttpResponse.class))).andReturn(future);
        })
        .expect(fireExceptionOnFailure)
        .expect(
            unit -> {
              DefaultFileRegion region = unit.mockConstructor(DefaultFileRegion.class,
                  new Class[]{FileChannel.class, long.class, long.class }, channel, 0L,
                  channel.size());

              unit.registerMock(DefaultFileRegion.class, region);
            })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.write(unit.get(DefaultFileRegion.class))).andReturn(future);
        })
        .expect(fireExceptionOnFailure)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)).andReturn(future);
        })
        .expect(noKeepAliveNoLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), bufferSize, keepAlive)
              .send(channel);
        });
  }

  @Test
  public void status() throws Exception {
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class)
        .expect(headers)
        .run(unit -> {
          NettyResponse rsp = new NettyResponse(unit.get(ChannelHandlerContext.class),
              bufferSize, true);
          assertEquals(200, rsp.statusCode());
          rsp.statusCode(201);
          assertEquals(201, rsp.statusCode());
        });
  }

  @Test
  public void end() throws Exception {
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class)
        .expect(headers)
        .expect(unit -> {
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.attr(NettyWebSocket.KEY)).andReturn(null);
        })
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultFullHttpResponse.class,
              new Class[]{HttpVersion.class, HttpResponseStatus.class },
              HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(unit.get(DefaultHttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(unit -> {
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(unit.get(HttpResponse.class))).andReturn(
              unit.get(ChannelFuture.class));
        })
        .expect(noKeepAliveNoLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class),
              bufferSize, true)
              .end();
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void end2() throws Exception {
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class)
        .expect(headers)
        .expect(unit -> {
          Attribute<NettyWebSocket> attr = unit.mock(Attribute.class);
          expect(attr.get()).andReturn(null);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.attr(NettyWebSocket.KEY)).andReturn(attr);
        })
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultFullHttpResponse.class,
              new Class[]{HttpVersion.class, HttpResponseStatus.class },
              HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(unit.get(DefaultHttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(unit -> {
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(unit.get(HttpResponse.class))).andReturn(
              unit.get(ChannelFuture.class));
        })
        .expect(noKeepAliveNoLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class),
              bufferSize, true)
              .end();
        });
  }

  private FileChannel newFileChannel(final int size) {
    return new FileChannel() {
      @Override
      public int read(final ByteBuffer dst) throws IOException {
        return 0;
      }

      @Override
      public long read(final ByteBuffer[] dsts, final int offset, final int length)
          throws IOException {
        return 0;
      }

      @Override
      public int write(final ByteBuffer src) throws IOException {
        return 0;
      }

      @Override
      public long write(final ByteBuffer[] srcs, final int offset, final int length)
          throws IOException {
        return 0;
      }

      @Override
      public long position() throws IOException {
        return 0;
      }

      @Override
      public FileChannel position(final long newPosition) throws IOException {
        return null;
      }

      @Override
      public long size() throws IOException {
        return size;
      }

      @Override
      public FileChannel truncate(final long size) throws IOException {
        return null;
      }

      @Override
      public void force(final boolean metaData) throws IOException {
      }

      @Override
      public long transferTo(final long position, final long count, final WritableByteChannel target)
          throws IOException {
        return 0;
      }

      @Override
      public long transferFrom(final ReadableByteChannel src, final long position, final long count)
          throws IOException {
        return 0;
      }

      @Override
      public int read(final ByteBuffer dst, final long position) throws IOException {
        return 0;
      }

      @Override
      public int write(final ByteBuffer src, final long position) throws IOException {
        return 0;
      }

      @Override
      public MappedByteBuffer map(final MapMode mode, final long position, final long size)
          throws IOException {
        return null;
      }

      @Override
      public FileLock lock(final long position, final long size, final boolean shared)
          throws IOException {
        return null;
      }

      @Override
      public FileLock tryLock(final long position, final long size, final boolean shared)
          throws IOException {
        return null;
      }

      @Override
      protected void implCloseChannel() throws IOException {
      }

    };
  }

}

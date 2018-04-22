package org.jooby.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import static io.netty.channel.ChannelFutureListener.CLOSE;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.Attribute;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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

@RunWith(PowerMockRunner.class)
@PrepareForTest({NettyResponse.class, DefaultFullHttpResponse.class, Unpooled.class,
    DefaultHttpHeaders.class, DefaultHttpResponse.class, ChunkedStream.class,
    DefaultFileRegion.class})
public class NettyResponseTest {

  private Block channel = unit -> {
    Channel channel = unit.mock(Channel.class);
    unit.registerMock(Channel.class, channel);

    ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
    expect(ctx.channel()).andReturn(channel).times(1, 2);
  };

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
  };

  private Block deflen = unit -> {
    HttpHeaders headers = unit.get(HttpHeaders.class);

    ByteBuf buffer = unit.get(ByteBuf.class);
    expect(buffer.readableBytes()).andReturn(bytes.length);

    expect(headers.remove(HttpHeaderNames.TRANSFER_ENCODING)).andReturn(headers);
    expect(headers.set(HttpHeaderNames.CONTENT_LENGTH, bytes.length)).andReturn(headers);
  };

  private Block connkeep = unit -> {
    HttpHeaders headers = unit.get(HttpHeaders.class);

    expect(headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)).andReturn(headers);
  };

  private Block fullResponse = unit -> {
    DefaultFullHttpResponse rsp = unit.mockConstructor(
        DefaultFullHttpResponse.class,
        new Class[]{HttpVersion.class, HttpResponseStatus.class, ByteBuf.class},
        HttpVersion.HTTP_1_1, HttpResponseStatus.OK, unit.get(ByteBuf.class));

    HttpHeaders headers = unit.mock(HttpHeaders.class);
    expect(headers.set(unit.get(HttpHeaders.class))).andReturn(headers);

    expect(rsp.headers()).andReturn(headers);

    unit.registerMock(HttpResponse.class, rsp);
  };

  private Block keeAliveWithLen = unit -> {
  };

  private Block noKeepAlive = unit -> {
    ChannelFuture future = unit.get(ChannelFuture.class);

    expect(future.addListener(CLOSE)).andReturn(future);
  };

  private Block noKeepAliveNoLen = unit -> {
    ChannelFuture future = unit.get(ChannelFuture.class);

    expect(future.addListener(CLOSE)).andReturn(future);
  };

  @SuppressWarnings("unchecked")
  private Block async = unit -> {
    Channel channel = unit.get(Channel.class);

    Attribute<Boolean> async = unit.mock(Attribute.class);
    expect(async.get()).andReturn(false);
    expect(channel.attr(NettyRequest.ASYNC)).andReturn(async);
  };

  @SuppressWarnings("unchecked")
  private Block setNeedFlush = unit -> {
    Attribute<Boolean> needFlush = unit.mock(Attribute.class);
    needFlush.set(false);

    Channel channel = unit.get(Channel.class);
    expect(channel.attr(NettyRequest.NEED_FLUSH)).andReturn(needFlush);
  };

  @Test
  public void defaults() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class, HttpHeaders.class)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
              bufferSize, keepAlive);
        });
  }

  @Test
  public void headers() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    List<String> v = Arrays.asList("h1");
    new MockUnit(ChannelHandlerContext.class, HttpHeaders.class)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.getAll("h")).andReturn(v);
        })
        .run(unit -> {
          assertEquals(v,
              new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
                  bufferSize,
                  keepAlive)
                  .headers("h"));
        });
  }

  @Test
  public void noHeaders() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    List<String> v = Collections.emptyList();
    new MockUnit(ChannelHandlerContext.class, HttpHeaders.class)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.getAll("h")).andReturn(null);
        })
        .run(unit -> {
          assertEquals(v,
              new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
                  bufferSize,
                  keepAlive)
                  .headers("h"));
        });
  }

  @Test
  public void header() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class, HttpHeaders.class)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.get("h")).andReturn("v");
        })
        .run(unit -> {
          assertEquals(Optional.of("v"),
              new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
                  bufferSize, keepAlive)
                  .header("h"));
        });
  }

  @Test
  public void noHeader() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class, HttpHeaders.class)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.get("h")).andReturn(null);
        })
        .run(unit -> {
          assertEquals(Optional.empty(),
              new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
                  bufferSize, keepAlive)
                  .header("h"));
        });
  }

  @Test
  public void setHeader() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class, HttpHeaders.class)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.set("h", "v")).andReturn(headers);
        })
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
              bufferSize, keepAlive)
              .header("h", "v");
        });
  }

  @Test
  public void setHeaders() throws Exception {
    int bufferSize = 8192;
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class, HttpHeaders.class)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.remove("h")).andReturn(headers);
          expect(headers.add("h", Arrays.asList("v1", "v2"))).andReturn(headers);
        })
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
              bufferSize, keepAlive)
              .header("h", Arrays.asList("v1", "v2"));
        });
  }

  @Test
  public void sendBytesSetDefLenAndKeepAlive() throws Exception {
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, HttpHeaders.class)
        .expect(channel)
        .expect(wrapBytes)
        .expect(headers)
        .expect(deflen)
        .expect(connkeep)
        .expect(fullResponse)
        .expect(async)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.voidPromise()).andReturn(promise);
          expect(ctx.write(unit.get(HttpResponse.class), promise)).andReturn(future);
        })
        .expect(keeAliveWithLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
              bufferSize, keepAlive)
              .send(bytes);
        });
  }

  @Test
  public void sendBufferSetDefLenAndKeepAlive() throws Exception {
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, HttpHeaders.class)
        .expect(channel)
        .expect(wrapBuffer)
        .expect(headers)
        .expect(deflen)
        .expect(connkeep)
        .expect(fullResponse)
        .expect(async)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.voidPromise()).andReturn(promise);
          expect(ctx.write(unit.get(HttpResponse.class), promise)).andReturn(future);
        })
        .expect(keeAliveWithLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
              bufferSize, keepAlive)
              .send(buffer);
        });
  }

  @Test
  public void sendBytesKeepAlive() throws Exception {
    boolean keepAlive = true;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, HttpHeaders.class)
        .expect(channel)
        .expect(wrapBytes)
        .expect(headers)
        .expect(deflen)
        .expect(connkeep)
        .expect(fullResponse)
        .expect(async)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.voidPromise()).andReturn(promise);
          expect(ctx.write(unit.get(HttpResponse.class), promise)).andReturn(future);
        })
        .expect(keeAliveWithLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
              bufferSize, keepAlive)
              .send(bytes);
        });
  }

  @Test
  public void sendBytesNoKeepAlive() throws Exception {
    boolean keepAlive = false;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, HttpHeaders.class)
        .expect(channel)
        .expect(wrapBytes)
        .expect(headers)
        .expect(deflen)
        .expect(fullResponse)
        .expect(async)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.newPromise()).andReturn(promise);
          expect(ctx.write(unit.get(HttpResponse.class), promise)).andReturn(future);
        })
        .expect(noKeepAlive)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
              bufferSize, keepAlive)
              .send(bytes);
        });
  }

  @Test
  public void sendBytesKeepAliveOffNoLen() throws Exception {
    boolean keepAlive = false;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, HttpHeaders.class)
        .expect(channel)
        .expect(wrapBytes)
        .expect(headers)
        .expect(deflen)
        .expect(fullResponse)
        .expect(async)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.newPromise()).andReturn(promise);
          expect(ctx.write(unit.get(HttpResponse.class), promise)).andReturn(future);
        })
        .expect(noKeepAliveNoLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
              bufferSize, keepAlive)
              .send(bytes);
        });
  }

  @Test
  public void sendEmptyStream() throws Exception {
    boolean keepAlive = false;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, InputStream.class,
        HttpHeaders.class)
        .expect(unit -> {
          InputStream stream = unit.get(InputStream.class);
          expect(stream.read(unit.capture(byte[].class), eq(0), eq(bufferSize))).andReturn(-1);
        })
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
              bufferSize, keepAlive)
              .send(unit.get(InputStream.class));
        }, unit -> {
          assertEquals(bufferSize, unit.captured(byte[].class).iterator().next().length);
        });
  }

  @Test
  public void sendHeadChunk() throws Exception {
    boolean keepAlive = false;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, InputStream.class,
        HttpHeaders.class)
        .expect(channel)
        .expect(unit -> {
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
        .expect(deflen)
        .expect(fullResponse)
        .expect(async)
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.newPromise()).andReturn(promise);
          expect(ctx.write(unit.get(HttpResponse.class), promise)).andReturn(future);
        })
        .expect(noKeepAliveNoLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
              bytes.length, keepAlive)
              .send(unit.get(InputStream.class));
        });
  }

  @Test
  public void sendChunks() throws Exception {
    boolean keepAlive = true;
    int bufferSize = 10;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, InputStream.class,
        HttpHeaders.class)
        .expect(channel)
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
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.contains(HttpHeaderNames.CONTENT_LENGTH)).andReturn(false);
          expect(headers.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED))
              .andReturn(headers);
        })
        .expect(setNeedFlush)
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultHttpResponse.class,
              new Class[]{HttpVersion.class,
                  HttpResponseStatus.class},
              HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(unit.get(HttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(unit -> {
          EventLoop loop = unit.mock(EventLoop.class);
          loop.execute(unit.capture(Runnable.class));

          Channel channel = unit.get(Channel.class);
          expect(channel.eventLoop()).andReturn(loop);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.write(unit.get(HttpResponse.class))).andReturn(future);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.write(unit.get(ByteBuf.class))).andReturn(future);
        })
        .expect(unit -> {
          ChunkedStream chunkedStream = unit.mockConstructor(ChunkedStream.class, new Class[]{
              InputStream.class, int.class}, unit.get(InputStream.class), bufferSize);
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.write(chunkedStream)).andReturn(future);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.newPromise()).andReturn(promise);
          expect(ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, promise)).andReturn(future);
        })
        .expect(unit -> {
          ChannelPipeline pipeline = unit.mock(ChannelPipeline.class);
          expect(pipeline.get("chunker")).andReturn(null);
          expect(pipeline.addAfter(eq("codec"), eq("chunker"), isA(ChunkedWriteHandler.class)))
              .andReturn(pipeline);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.pipeline()).andReturn(pipeline);
        })
        .expect(noKeepAliveNoLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
              bufferSize, keepAlive)
              .send(unit.get(InputStream.class));
        }, unit -> {
          unit.captured(Runnable.class).iterator().next().run();
        });
  }

  @Test
  public void sendChunksNoKeepAlive() throws Exception {
    boolean keepAlive = false;
    int bufferSize = 10;
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, InputStream.class,
        HttpHeaders.class)
        .expect(channel)
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
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.contains(HttpHeaderNames.CONTENT_LENGTH)).andReturn(true);
        })
        .expect(setNeedFlush)
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultHttpResponse.class,
              new Class[]{HttpVersion.class,
                  HttpResponseStatus.class},
              HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(unit.get(HttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(unit -> {
          EventLoop loop = unit.mock(EventLoop.class);
          loop.execute(unit.capture(Runnable.class));

          Channel channel = unit.get(Channel.class);
          expect(channel.eventLoop()).andReturn(loop);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.write(unit.get(HttpResponse.class))).andReturn(future);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.write(unit.get(ByteBuf.class))).andReturn(future);
        })
        .expect(unit -> {
          ChunkedStream chunkedStream = unit.mockConstructor(ChunkedStream.class, new Class[]{
              InputStream.class, int.class}, unit.get(InputStream.class), bufferSize);
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.write(chunkedStream)).andReturn(future);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.newPromise()).andReturn(promise);
          expect(ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, promise)).andReturn(future);
        })
        .expect(unit -> {
          ChannelPipeline pipeline = unit.mock(ChannelPipeline.class);
          expect(pipeline.get("chunker")).andReturn(null);
          expect(pipeline.addAfter(eq("codec"), eq("chunker"), isA(ChunkedWriteHandler.class)))
              .andReturn(pipeline);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.pipeline()).andReturn(pipeline);
        })
        .expect(noKeepAliveNoLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
              bufferSize, keepAlive)
              .send(unit.get(InputStream.class));
        }, unit -> {
          unit.captured(Runnable.class).iterator().next().run();
        });
  }

  @Test
  public void sendFileChannelNoKeepAlive() throws Exception {
    boolean keepAlive = false;
    FileChannel fchannel = newFileChannel(8192);
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, HttpHeaders.class)
        .expect(channel)
        .expect(headers)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.remove(HttpHeaderNames.TRANSFER_ENCODING)).andReturn(headers);
          expect(headers.set(HttpHeaderNames.CONTENT_LENGTH, 8192L)).andReturn(headers);
        })
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultHttpResponse.class,
              new Class[]{HttpVersion.class,
                  HttpResponseStatus.class},
              HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(unit.get(HttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.voidPromise()).andReturn(promise);
          expect(ctx.write(unit.get(HttpResponse.class), promise)).andReturn(future);
        })
        .expect(unit -> {
          DefaultFileRegion region = unit.mockConstructor(DefaultFileRegion.class,
              new Class[]{FileChannel.class, long.class, long.class}, fchannel, 0L,
              fchannel.size());

          unit.registerMock(DefaultFileRegion.class, region);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.voidPromise()).andReturn(promise);
          expect(ctx.write(unit.get(DefaultFileRegion.class), promise)).andReturn(future);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)).andReturn(future);
        })
        .expect(setNeedFlush)
        .expect(unit -> {
          EventLoop loop = unit.mock(EventLoop.class);
          loop.execute(unit.capture(Runnable.class));

          Channel chn = unit.get(Channel.class);
          expect(chn.eventLoop()).andReturn(loop);

        })
        .expect(unit -> {
          ChannelPipeline pipeline = unit.mock(ChannelPipeline.class);
          expect(pipeline.get(SslHandler.class)).andReturn(null);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.pipeline()).andReturn(pipeline);
        })
        .expect(noKeepAliveNoLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
              bufferSize, keepAlive)
              .send(fchannel);
        }, unit -> {
          unit.captured(Runnable.class).iterator().next().run();
        });
  }

  @Test
  public void sendFileChannelKeepAlive() throws Exception {
    boolean keepAlive = true;
    FileChannel fchannel = newFileChannel(8192);
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, HttpHeaders.class)
        .expect(channel)
        .expect(headers)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.remove(HttpHeaderNames.TRANSFER_ENCODING)).andReturn(headers);
          expect(headers.set(HttpHeaderNames.CONTENT_LENGTH, 8192L)).andReturn(headers);
          expect(headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE))
              .andReturn(headers);
        })
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultHttpResponse.class,
              new Class[]{HttpVersion.class,
                  HttpResponseStatus.class},
              HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(unit.get(HttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.voidPromise()).andReturn(promise);
          expect(ctx.write(unit.get(HttpResponse.class), promise)).andReturn(future);
        })
        .expect(unit -> {
          DefaultFileRegion region = unit.mockConstructor(DefaultFileRegion.class,
              new Class[]{FileChannel.class, long.class, long.class}, fchannel, 0L,
              fchannel.size());

          unit.registerMock(DefaultFileRegion.class, region);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.voidPromise()).andReturn(promise);
          expect(ctx.write(unit.get(DefaultFileRegion.class), promise)).andReturn(future);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.voidPromise()).andReturn(promise);
          expect(ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, promise)).andReturn(future);
        })
        .expect(setNeedFlush)
        .expect(unit -> {
          EventLoop loop = unit.mock(EventLoop.class);
          loop.execute(unit.capture(Runnable.class));

          Channel chn = unit.get(Channel.class);
          expect(chn.eventLoop()).andReturn(loop);

        })
        .expect(unit -> {
          ChannelPipeline pipeline = unit.mock(ChannelPipeline.class);
          expect(pipeline.get(SslHandler.class)).andReturn(null);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.pipeline()).andReturn(pipeline);
        })
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class), bufferSize, keepAlive)
              .send(fchannel);
        }, unit -> {
          unit.captured(Runnable.class).iterator().next().run();
        });
  }

  @Test
  public void sendFileChannelSSLNoKeepAlive() throws Exception {
    boolean keepAlive = false;
    FileChannel fchannel = newFileChannel(8192);
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, HttpHeaders.class)
        .expect(channel)
        .expect(headers)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.remove(HttpHeaderNames.TRANSFER_ENCODING)).andReturn(headers);
          expect(headers.set(HttpHeaderNames.CONTENT_LENGTH, 8192L)).andReturn(headers);
        })
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultHttpResponse.class,
              new Class[]{HttpVersion.class,
                  HttpResponseStatus.class},
              HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(unit.get(HttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.voidPromise()).andReturn(promise);
          expect(ctx.write(unit.get(HttpResponse.class), promise)).andReturn(future);
        })
        .expect(unit -> {
          ChunkedNioFile nioFile = unit.mockConstructor(ChunkedNioFile.class,
              new Class[]{FileChannel.class, long.class, long.class, int.class},
              fchannel, 0L, fchannel.size(), 8192);

          HttpChunkedInput chunked = unit.mockConstructor(HttpChunkedInput.class,
              new Class[]{ChunkedInput.class}, nioFile);

          unit.registerMock(HttpChunkedInput.class, chunked);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.writeAndFlush(unit.get(HttpChunkedInput.class))).andReturn(future);
        })
        .expect(setNeedFlush)
        .expect(unit -> {
          EventLoop loop = unit.mock(EventLoop.class);
          loop.execute(unit.capture(Runnable.class));

          Channel chn = unit.get(Channel.class);
          expect(chn.eventLoop()).andReturn(loop);

        })
        .expect(unit -> {
          ChannelPipeline pipeline = unit.mock(ChannelPipeline.class);
          expect(pipeline.get("chunker")).andReturn(null);
          expect(pipeline.get(SslHandler.class)).andReturn(unit.mock(SslHandler.class));
          expect(pipeline.addAfter(eq("codec"), eq("chunker"), isA(ChunkedWriteHandler.class)))
              .andReturn(pipeline);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.pipeline()).andReturn(pipeline);
        })
        .expect(noKeepAliveNoLen)
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class), bufferSize, keepAlive)
              .send(fchannel);
        }, unit -> {
          unit.captured(Runnable.class).iterator().next().run();
        });
  }

  @Test
  public void sendFileChannelSSLKeepAlive() throws Exception {
    boolean keepAlive = true;
    FileChannel fchannel = newFileChannel(8192);
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, HttpHeaders.class)
        .expect(channel)
        .expect(headers)
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.remove(HttpHeaderNames.TRANSFER_ENCODING)).andReturn(headers);
          expect(headers.set(HttpHeaderNames.CONTENT_LENGTH, 8192L)).andReturn(headers);
          expect(headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE))
              .andReturn(headers);
        })
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultHttpResponse.class,
              new Class[]{HttpVersion.class,
                  HttpResponseStatus.class},
              HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.set(unit.get(HttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.voidPromise()).andReturn(promise);
          expect(ctx.write(unit.get(HttpResponse.class), promise)).andReturn(future);
        })
        .expect(unit -> {
          ChunkedNioFile nioFile = unit.mockConstructor(ChunkedNioFile.class,
              new Class[]{FileChannel.class, long.class, long.class, int.class},
              fchannel, 0L, fchannel.size(), 8192);

          HttpChunkedInput chunked = unit.mockConstructor(HttpChunkedInput.class,
              new Class[]{ChunkedInput.class}, nioFile);

          unit.registerMock(HttpChunkedInput.class, chunked);
        })
        .expect(unit -> {
          ChannelFuture future = unit.get(ChannelFuture.class);

          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.voidPromise()).andReturn(promise);
          expect(ctx.writeAndFlush(unit.get(HttpChunkedInput.class), promise)).andReturn(future);
        })
        .expect(setNeedFlush)
        .expect(unit -> {
          EventLoop loop = unit.mock(EventLoop.class);
          loop.execute(unit.capture(Runnable.class));

          Channel chn = unit.get(Channel.class);
          expect(chn.eventLoop()).andReturn(loop);

        })
        .expect(unit -> {
          ChannelPipeline pipeline = unit.mock(ChannelPipeline.class);
          expect(pipeline.get("chunker")).andReturn(null);
          expect(pipeline.get(SslHandler.class)).andReturn(unit.mock(SslHandler.class));
          expect(pipeline.addAfter(eq("codec"), eq("chunker"), isA(ChunkedWriteHandler.class)))
              .andReturn(pipeline);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.pipeline()).andReturn(pipeline);
        })
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class), bufferSize, keepAlive)
              .send(fchannel);
        }, unit -> {
          unit.captured(Runnable.class).iterator().next().run();
        });
  }

  @Test
  public void status() throws Exception {
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, HttpHeaders.class)
        .expect(headers)
        .run(unit -> {
          NettyResponse rsp = new NettyResponse(unit.get(ChannelHandlerContext.class),
              unit.get(HttpHeaders.class), bufferSize, true);
          assertEquals(200, rsp.statusCode());
          rsp.statusCode(201);
          assertEquals(201, rsp.statusCode());
        });
  }

  @Test
  public void end() throws Exception {
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, HttpHeaders.class)
        .expect(channel)
        .expect(headers)
        .expect(unit -> {
          Channel ctx = unit.get(Channel.class);
          expect(ctx.attr(NettyWebSocket.KEY)).andReturn(null);
        })
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.set(HttpHeaderNames.CONTENT_LENGTH, 0)).andReturn(headers);
        })
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultFullHttpResponse.class,
              new Class[]{HttpVersion.class, HttpResponseStatus.class},
              HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(unit.get(HttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(unit -> {
          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.voidPromise()).andReturn(promise);
          expect(ctx.write(unit.get(HttpResponse.class), promise))
              .andReturn(unit.get(ChannelFuture.class));
        })
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class), bufferSize, true)
              .end();
        });
  }

  @Test
  public void endNoKeepAlive() throws Exception {
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, HttpHeaders.class)
        .expect(channel)
        .expect(headers)
        .expect(unit -> {
          Channel ctx = unit.get(Channel.class);
          expect(ctx.attr(NettyWebSocket.KEY)).andReturn(null);
        })
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.set(HttpHeaderNames.CONTENT_LENGTH, 0)).andReturn(headers);
        })
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultFullHttpResponse.class,
              new Class[]{HttpVersion.class, HttpResponseStatus.class},
              HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(unit.get(HttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(noKeepAliveNoLen)
        .expect(unit -> {
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.write(unit.get(HttpResponse.class))).andReturn(unit.get(ChannelFuture.class));
        })
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class), bufferSize, false)
              .end();
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void end2() throws Exception {
    new MockUnit(ChannelHandlerContext.class, ByteBuf.class, ChannelFuture.class, HttpHeaders.class)
        .expect(headers)
        .expect(channel)
        .expect(unit -> {
          Attribute<NettyWebSocket> attr = unit.mock(Attribute.class);
          expect(attr.get()).andReturn(null);

          Channel ctx = unit.get(Channel.class);
          expect(ctx.attr(NettyWebSocket.KEY)).andReturn(attr);
        })
        .expect(unit -> {
          HttpHeaders headers = unit.get(HttpHeaders.class);
          expect(headers.set(HttpHeaderNames.CONTENT_LENGTH, 0)).andReturn(headers);
        })
        .expect(unit -> {
          DefaultHttpResponse rsp = unit.mockConstructor(DefaultFullHttpResponse.class,
              new Class[]{HttpVersion.class, HttpResponseStatus.class},
              HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

          HttpHeaders headers = unit.mock(HttpHeaders.class);
          expect(headers.set(unit.get(HttpHeaders.class))).andReturn(headers);

          expect(rsp.headers()).andReturn(headers);

          unit.registerMock(HttpResponse.class, rsp);
        })
        .expect(unit -> {
          ChannelPromise promise = unit.mock(ChannelPromise.class);
          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.voidPromise()).andReturn(promise);
          expect(ctx.write(unit.get(HttpResponse.class), promise))
              .andReturn(unit.get(ChannelFuture.class));
        })
        .run(unit -> {
          new NettyResponse(unit.get(ChannelHandlerContext.class), unit.get(HttpHeaders.class),
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
      public long transferTo(final long position, final long count,
          final WritableByteChannel target)
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

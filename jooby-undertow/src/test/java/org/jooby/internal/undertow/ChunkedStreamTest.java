package org.jooby.internal.undertow;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.ServerConnection;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.xnio.Pool;
import org.xnio.Pooled;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ChunkedStream.class, HttpServerExchange.class, HeaderMap.class })
public class ChunkedStreamTest {

  int bufferSize = 10;
  byte[] bytes = "bytes".getBytes();
  ByteBuffer buffer = ByteBuffer.wrap(bytes);

  @SuppressWarnings("unchecked")
  private Block exchange = unit -> {
    Pool<ByteBuffer> pool = unit.mock(Pool.class);
    expect(pool.allocate()).andReturn(unit.get(Pooled.class));

    ServerConnection conn = unit.mock(ServerConnection.class);
    expect(conn.getBufferPool()).andReturn(pool);
    expect(conn.getBufferSize()).andReturn(bufferSize);

    HttpServerExchange exchange = unit.get(HttpServerExchange.class);
    expect(exchange.getResponseSender()).andReturn(unit.get(Sender.class));
    expect(exchange.getConnection()).andReturn(conn);
  };

  private Block noIoThread = unit -> {
    HttpServerExchange exchange = unit.get(HttpServerExchange.class);
    expect(exchange.isInIoThread()).andReturn(false);
  };

  private Block ioThread = unit -> {
    HttpServerExchange exchange = unit.get(HttpServerExchange.class);
    expect(exchange.isInIoThread()).andReturn(true);
    expect(exchange.dispatch(isA(ChunkedStream.class))).andReturn(exchange);
  };

  private Block nolen = unit -> {
    HeaderMap headers = unit.mock(HeaderMap.class);
    expect(headers.contains(Headers.CONTENT_LENGTH)).andReturn(false);
    expect(headers.put(Headers.CONTENT_LENGTH, bytes.length)).andReturn(headers);
    expect(headers.remove(Headers.TRANSFER_ENCODING)).andReturn(null);

    HttpServerExchange exchange = unit.get(HttpServerExchange.class);
    expect(exchange.getResponseHeaders()).andReturn(headers);
  };

  private Block len = unit -> {
    HeaderMap headers = unit.mock(HeaderMap.class);
    expect(headers.contains(Headers.CONTENT_LENGTH)).andReturn(true);

    HttpServerExchange exchange = unit.get(HttpServerExchange.class);
    expect(exchange.getResponseHeaders()).andReturn(headers);
  };

  private Block transferEncoding = unit -> {
    HeaderMap headers = unit.mock(HeaderMap.class);
    expect(headers.contains(Headers.CONTENT_LENGTH)).andReturn(false);
    expect(headers.put(Headers.TRANSFER_ENCODING, "chunked")).andReturn(headers);

    HttpServerExchange exchange = unit.get(HttpServerExchange.class);
    expect(exchange.getResponseHeaders()).andReturn(headers);
  };

  private Block sendChunk = unit -> {
    Sender sender = unit.get(Sender.class);

    sender.send(eq(buffer), isA(ChunkedStream.class));
  };

  private Block readChunk = unit -> {
    ReadableByteChannel channel = unit.get(ReadableByteChannel.class);
    expect(channel.read(buffer)).andReturn(bytes.length);
  };

  private Block readNoChunk = unit -> {
    ReadableByteChannel channel = unit.get(ReadableByteChannel.class);
    expect(channel.read(buffer)).andReturn(-1);
  };

  private Block readErrChunk = unit -> {
    ReadableByteChannel channel = unit.get(ReadableByteChannel.class);
    expect(channel.read(buffer)).andThrow(new IOException("intentional err"));
  };

  private Block readLargeChunk = unit -> {
    ReadableByteChannel channel = unit.get(ReadableByteChannel.class);
    expect(channel.read(buffer)).andReturn(bufferSize);
  };

  @SuppressWarnings("unchecked")
  private Block pooled = unit -> {
    Pooled<ByteBuffer> pooled = unit.get(Pooled.class);
    expect(pooled.getResource()).andReturn(buffer);
  };

  @Test
  public void sendNoIoThread() throws Exception {
    new MockUnit(ReadableByteChannel.class, HttpServerExchange.class, IoCallback.class,
        Sender.class, Pooled.class)
        .expect(exchange)
        .expect(noIoThread)
        .expect(pooled)
        .expect(readChunk)
        .expect(nolen)
        .expect(sendChunk)
        .run(unit -> {
          new ChunkedStream().send(unit.get(ReadableByteChannel.class),
              unit.get(HttpServerExchange.class), unit.get(IoCallback.class));
        });
  }

  @Test
  public void sendNoIoThreadWithLen() throws Exception {
    new MockUnit(ReadableByteChannel.class, HttpServerExchange.class, IoCallback.class,
        Sender.class, Pooled.class)
        .expect(exchange)
        .expect(noIoThread)
        .expect(pooled)
        .expect(readChunk)
        .expect(unit -> {
          HeaderMap headers = unit.mock(HeaderMap.class);
          expect(headers.contains(Headers.CONTENT_LENGTH)).andReturn(true);

          HttpServerExchange exchange = unit.get(HttpServerExchange.class);
          expect(exchange.getResponseHeaders()).andReturn(headers);
        })
        .expect(sendChunk)
        .run(unit -> {
          new ChunkedStream().send(unit.get(ReadableByteChannel.class),
              unit.get(HttpServerExchange.class), unit.get(IoCallback.class));
        });
  }

  @Test
  public void sendNoIoThread2Chunks() throws Exception {
    new MockUnit(ReadableByteChannel.class, HttpServerExchange.class, IoCallback.class,
        Sender.class, Pooled.class)
        .expect(exchange)
        .expect(noIoThread)
        .expect(pooled)
        .expect(readLargeChunk)
        .expect(transferEncoding)
        .expect(sendChunk)
        .expect(noIoThread)
        .expect(pooled)
        .expect(readChunk)
        .expect(sendChunk)
        .run(unit -> {
          ChunkedStream chunkedStream = new ChunkedStream();
          chunkedStream.send(unit.get(ReadableByteChannel.class),
              unit.get(HttpServerExchange.class), unit.get(IoCallback.class));
          chunkedStream.onComplete(unit.get(HttpServerExchange.class), unit.get(Sender.class));
        });
  }

  @Test
  public void sendNoIoThread2ChunksWithLen() throws Exception {
    new MockUnit(ReadableByteChannel.class, HttpServerExchange.class, IoCallback.class,
        Sender.class, Pooled.class)
        .expect(exchange)
        .expect(noIoThread)
        .expect(pooled)
        .expect(readLargeChunk)
        .expect(len)
        .expect(sendChunk)
        .expect(noIoThread)
        .expect(pooled)
        .expect(readChunk)
        .expect(sendChunk)
        .run(unit -> {
          ChunkedStream chunkedStream = new ChunkedStream();
          chunkedStream.send(unit.get(ReadableByteChannel.class),
              unit.get(HttpServerExchange.class), unit.get(IoCallback.class));
          chunkedStream.onComplete(unit.get(HttpServerExchange.class), unit.get(Sender.class));
        });
  }

  @Test
  public void sendIoThread() throws Exception {
    new MockUnit(ReadableByteChannel.class, HttpServerExchange.class, IoCallback.class,
        Sender.class, Pooled.class)
        .expect(exchange)
        .expect(ioThread)
        .expect(pooled)
        .expect(readChunk)
        .expect(nolen)
        .expect(sendChunk)
        .run(unit -> {
          ChunkedStream chunkedStream = new ChunkedStream();
          chunkedStream.send(unit.get(ReadableByteChannel.class),
              unit.get(HttpServerExchange.class), unit.get(IoCallback.class));
          chunkedStream.run();
        });
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void sendNoIoThreadWithErr() throws Exception {
    new MockUnit(ReadableByteChannel.class, HttpServerExchange.class, IoCallback.class,
        Sender.class, Pooled.class)
        .expect(exchange)
        .expect(noIoThread)
        .expect(pooled)
        .expect(readErrChunk)
        .expect(unit -> {
          Pooled pooled = unit.get(Pooled.class);
          pooled.free();
          IoCallback err = unit.get(IoCallback.class);
          err.onException(eq(unit.get(HttpServerExchange.class)), eq(unit.get(Sender.class)),
              isA(IOException.class));
        })
        .run(unit -> {
          new ChunkedStream().send(unit.get(ReadableByteChannel.class),
              unit.get(HttpServerExchange.class), unit.get(IoCallback.class));
        });
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void sendDone() throws Exception {
    new MockUnit(ReadableByteChannel.class, HttpServerExchange.class, IoCallback.class,
        Sender.class, Pooled.class)
        .expect(exchange)
        .expect(noIoThread)
        .expect(pooled)
        .expect(readNoChunk)
        .expect(unit -> {
          Pooled pooled = unit.get(Pooled.class);
          pooled.free();
          IoCallback success = unit.get(IoCallback.class);
          success.onComplete(eq(unit.get(HttpServerExchange.class)), eq(unit.get(Sender.class)));
        })
        .run(unit -> {
          new ChunkedStream().send(unit.get(ReadableByteChannel.class),
              unit.get(HttpServerExchange.class), unit.get(IoCallback.class));
        });
  }

}

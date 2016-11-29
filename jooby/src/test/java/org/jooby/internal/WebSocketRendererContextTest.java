package org.jooby.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.WebSocket;
import org.jooby.spi.NativeWebSocket;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.collect.Lists;

public class WebSocketRendererContextTest {

  @Test(expected = UnsupportedOperationException.class)
  public void fileChannel() throws Exception {
    MediaType produces = MediaType.json;
    new MockUnit(Renderer.class, NativeWebSocket.class, WebSocket.SuccessCallback.class,
        WebSocket.ErrCallback.class)
        .run(unit -> {
          WebSocketRendererContext ctx = new WebSocketRendererContext(
              Lists.newArrayList(unit.get(Renderer.class)),
              unit.get(NativeWebSocket.class),
              produces,
              StandardCharsets.UTF_8,
              Locale.US,
              unit.get(WebSocket.SuccessCallback.class),
              unit.get(WebSocket.ErrCallback.class));
          ctx.send(newFileChannel());
        });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void inputStream() throws Exception {
    MediaType produces = MediaType.json;
    new MockUnit(Renderer.class, NativeWebSocket.class, WebSocket.SuccessCallback.class,
        WebSocket.ErrCallback.class, InputStream.class)
        .run(unit -> {
          WebSocketRendererContext ctx = new WebSocketRendererContext(
              Lists.newArrayList(unit.get(Renderer.class)),
              unit.get(NativeWebSocket.class),
              produces,
              StandardCharsets.UTF_8,
              Locale.US,
              unit.get(WebSocket.SuccessCallback.class),
              unit.get(WebSocket.ErrCallback.class));
          ctx.send(unit.get(InputStream.class));
        });
  }

  private FileChannel newFileChannel() {
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
        return 0;
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

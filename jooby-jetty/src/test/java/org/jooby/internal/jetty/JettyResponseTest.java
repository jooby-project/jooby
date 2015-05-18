package org.jooby.internal.jetty;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Response;
import org.jooby.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JettyResponse.class, Channels.class, LoggerFactory.class })
public class JettyResponseTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(Response.class)
        .run(unit -> {
          new JettyResponse(unit.get(Response.class));
        });
  }

  @Test
  public void sendBytes() throws Exception {
    byte[] bytes = "bytes".getBytes();
    new MockUnit(Response.class, HttpOutput.class)
        .expect(unit -> {
          HttpOutput output = unit.get(HttpOutput.class);
          output.sendContent(unit.capture(ByteBuffer.class), isA(JettyResponse.class));

          Response rsp = unit.get(Response.class);
          expect(rsp.getHttpOutput()).andReturn(output);
        })
        .run(unit -> {
          new JettyResponse(unit.get(Response.class))
              .send(bytes);
        }, unit -> {
          assertArrayEquals(bytes, unit.captured(ByteBuffer.class).iterator().next().array());
        });
  }

  @Test
  public void sendBuffer() throws Exception {
    byte[] bytes = "bytes".getBytes();
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    new MockUnit(Response.class, HttpOutput.class)
        .expect(unit -> {
          HttpOutput output = unit.get(HttpOutput.class);
          output.sendContent(eq(buffer), isA(JettyResponse.class));

          Response rsp = unit.get(Response.class);
          expect(rsp.getHttpOutput()).andReturn(output);
        })
        .run(unit -> {
          new JettyResponse(unit.get(Response.class))
              .send(buffer);
        });
  }

  @Test
  public void sendInputStream() throws Exception {
    new MockUnit(Response.class, HttpOutput.class, InputStream.class)
        .expect(unit -> {
          unit.mockStatic(Channels.class);
          ReadableByteChannel channel = unit.mock(ReadableByteChannel.class);
          expect(Channels.newChannel(unit.get(InputStream.class))).andReturn(channel);

          HttpOutput output = unit.get(HttpOutput.class);
          output.sendContent(eq(channel), isA(JettyResponse.class));

          Response rsp = unit.get(Response.class);
          expect(rsp.getHttpOutput()).andReturn(output);
        })
        .run(unit -> {
          new JettyResponse(unit.get(Response.class))
              .send(unit.get(InputStream.class));
        });
  }

  @Test
  public void sendFileChannel() throws Exception {
    FileChannel channel = newFileChannel();
    new MockUnit(Response.class, HttpOutput.class)
        .expect(unit -> {
          HttpOutput output = unit.get(HttpOutput.class);
          output.sendContent(eq(channel), isA(JettyResponse.class));

          Response rsp = unit.get(Response.class);
          expect(rsp.getHttpOutput()).andReturn(output);
        })
        .run(unit -> {
          new JettyResponse(unit.get(Response.class))
              .send(channel);
        });
  }

  @Test
  public void succeeded() throws Exception {
    new MockUnit(Response.class, HttpOutput.class)
        .run(unit -> {
          new JettyResponse(unit.get(Response.class))
              .succeeded();
        });
  }

  @Test
  public void failed() throws Exception {
    IOException cause = new IOException();
    new MockUnit(Response.class)
        .expect(unit -> {
          Logger log = unit.mock(Logger.class);
          log.error(unit.get(Response.class).toString(), cause);

          unit.mockStatic(LoggerFactory.class);
          expect(LoggerFactory.getLogger(org.jooby.Response.class)).andReturn(log);
        })
        .run(unit -> {
          new JettyResponse(unit.get(Response.class))
              .failed(cause);
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

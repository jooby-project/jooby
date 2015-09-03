package org.jooby.servlet;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.io.ByteStreams;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServletServletResponse.class, Channels.class, ByteStreams.class })
public class ServletServletResponseTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(HttpServletRequest.class, HttpServletResponse.class)
        .run(unit -> {
          new ServletServletResponse(unit.get(HttpServletRequest.class),
              unit.get(HttpServletResponse.class));
        });
  }

  @Test
  public void close() throws Exception {
    new MockUnit(HttpServletRequest.class, HttpServletResponse.class)
        .run(unit -> {
          new ServletServletResponse(unit.get(HttpServletRequest.class),
              unit.get(HttpServletResponse.class)).close();
        });
  }

  @Test
  public void headers() throws Exception {
    new MockUnit(HttpServletRequest.class, HttpServletResponse.class)
        .expect(unit -> {
          HttpServletResponse rsp = unit.get(HttpServletResponse.class);
          expect(rsp.getHeaders("h")).andReturn(Arrays.asList("v"));
        })
        .run(unit -> {
          assertEquals(Arrays.asList("v"),
              new ServletServletResponse(unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class)).headers("h"));
        });
  }

  @Test
  public void emptyHeaders() throws Exception {
    new MockUnit(HttpServletRequest.class, HttpServletResponse.class)
        .expect(unit -> {
          HttpServletResponse rsp = unit.get(HttpServletResponse.class);
          expect(rsp.getHeaders("h")).andReturn(Collections.emptyList());
        })
        .run(unit -> {
          assertEquals(Collections.emptyList(),
              new ServletServletResponse(unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class)).headers("h"));
        });
  }

  @Test
  public void noHeaders() throws Exception {
    new MockUnit(HttpServletRequest.class, HttpServletResponse.class)
        .expect(unit -> {
          HttpServletResponse rsp = unit.get(HttpServletResponse.class);
          expect(rsp.getHeaders("h")).andReturn(null);
        })
        .run(unit -> {
          assertEquals(Collections.emptyList(),
              new ServletServletResponse(unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class)).headers("h"));
        });
  }

  @Test
  public void header() throws Exception {
    new MockUnit(HttpServletRequest.class, HttpServletResponse.class)
        .expect(unit -> {
          HttpServletResponse rsp = unit.get(HttpServletResponse.class);
          expect(rsp.getHeader("h")).andReturn("v");
        })
        .run(unit -> {
          assertEquals(Optional.of("v"),
              new ServletServletResponse(unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class)).header("h"));
        });
  }

  @Test
  public void emptyHeader() throws Exception {
    new MockUnit(HttpServletRequest.class, HttpServletResponse.class)
        .expect(unit -> {
          HttpServletResponse rsp = unit.get(HttpServletResponse.class);
          expect(rsp.getHeader("h")).andReturn("");
        })
        .run(unit -> {
          assertEquals(Optional.empty(),
              new ServletServletResponse(unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class)).header("h"));
        });
  }

  @Test
  public void noHeader() throws Exception {
    new MockUnit(HttpServletRequest.class, HttpServletResponse.class)
        .expect(unit -> {
          HttpServletResponse rsp = unit.get(HttpServletResponse.class);
          expect(rsp.getHeader("h")).andReturn(null);
        })
        .run(unit -> {
          assertEquals(Optional.empty(),
              new ServletServletResponse(unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class)).header("h"));
        });
  }

  @Test
  public void sendBytes() throws Exception {
    byte[] bytes = "bytes".getBytes();
    new MockUnit(HttpServletRequest.class, HttpServletResponse.class, ServletOutputStream.class)
        .expect(unit -> {
          ServletOutputStream output = unit.get(ServletOutputStream.class);
          output.write(bytes);
          output.close();

          HttpServletResponse rsp = unit.get(HttpServletResponse.class);
          expect(rsp.getOutputStream()).andReturn(output);
        })
        .run(unit -> {
          new ServletServletResponse(unit.get(HttpServletRequest.class),
              unit.get(HttpServletResponse.class)).send(bytes);
        });
  }

  @Test
  public void sendByteBuffer() throws Exception {
    byte[] bytes = "bytes".getBytes();
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    new MockUnit(HttpServletRequest.class, HttpServletResponse.class, ServletOutputStream.class)
        .expect(unit -> {
          ServletOutputStream output = unit.get(ServletOutputStream.class);

          WritableByteChannel channel = unit.mock(WritableByteChannel.class);
          expect(channel.write(buffer)).andReturn(bytes.length);
          channel.close();

          unit.mockStatic(Channels.class);
          expect(Channels.newChannel(output)).andReturn(channel);

          HttpServletResponse rsp = unit.get(HttpServletResponse.class);
          expect(rsp.getOutputStream()).andReturn(output);
        })
        .run(unit -> {
          new ServletServletResponse(unit.get(HttpServletRequest.class),
              unit.get(HttpServletResponse.class)).send(buffer);
        });
  }

  @Test
  public void sendFileChannel() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    FileChannel fchannel = newFileChannel(latch);
    new MockUnit(HttpServletRequest.class, HttpServletResponse.class, ServletOutputStream.class)
        .expect(unit -> {
          ServletOutputStream output = unit.get(ServletOutputStream.class);

          WritableByteChannel channel = unit.mock(WritableByteChannel.class);

          unit.mockStatic(Channels.class);
          expect(Channels.newChannel(output)).andReturn(channel);

          unit.mockStatic(ByteStreams.class);
          expect(ByteStreams.copy(fchannel, channel)).andReturn(0L);

          fchannel.close();
          channel.close();

          HttpServletResponse rsp = unit.get(HttpServletResponse.class);
          expect(rsp.getOutputStream()).andReturn(output);
        })
        .run(unit -> {
          new ServletServletResponse(unit.get(HttpServletRequest.class),
              unit.get(HttpServletResponse.class)).send(fchannel);
        });
    latch.await();
  }

  @Test
  public void sendInputStream() throws Exception {
    new MockUnit(HttpServletRequest.class, HttpServletResponse.class, InputStream.class,
        ServletOutputStream.class)
            .expect(unit -> {
              InputStream in = unit.get(InputStream.class);
              ServletOutputStream output = unit.get(ServletOutputStream.class);

              unit.mockStatic(ByteStreams.class);
              expect(ByteStreams.copy(in, output)).andReturn(0L);

              output.close();
              in.close();

              HttpServletResponse rsp = unit.get(HttpServletResponse.class);
              expect(rsp.getOutputStream()).andReturn(output);
            })
            .run(unit -> {
              new ServletServletResponse(unit.get(HttpServletRequest.class),
                  unit.get(HttpServletResponse.class)).send(unit.get(InputStream.class));
            });
  }

  private FileChannel newFileChannel(final CountDownLatch latch) {
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
        latch.countDown();
      }

    };
  }

}

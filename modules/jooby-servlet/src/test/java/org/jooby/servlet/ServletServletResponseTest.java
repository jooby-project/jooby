package org.jooby.servlet;

import com.google.common.io.ByteStreams;
import static org.easymock.EasyMock.expect;
import org.jooby.funzy.Throwing;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServletServletResponse.class, Channels.class, ByteStreams.class,
    FileChannel.class, Throwing.class, Throwing.Runnable.class})
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
          rsp.setHeader("Transfer-Encoding", null);
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
    new MockUnit(HttpServletRequest.class, HttpServletResponse.class, ServletOutputStream.class)
        .expect(unit -> {
          FileChannel channel = unit.partialMock(FileChannel.class, "transferTo", "close");
          unit.registerMock(FileChannel.class, channel);
        })
        .expect(unit -> {
          FileChannel fchannel = unit.get(FileChannel.class);
          expect(fchannel.size()).andReturn(10L);
          ServletOutputStream output = unit.get(ServletOutputStream.class);

          WritableByteChannel channel = unit.mock(WritableByteChannel.class);

          unit.mockStatic(Channels.class);
          expect(Channels.newChannel(output)).andReturn(channel);

          expect(fchannel.transferTo(0L, 10L, channel)).andReturn(1L);
          fchannel.close();
          channel.close();

          HttpServletResponse rsp = unit.get(HttpServletResponse.class);
          expect(rsp.getOutputStream()).andReturn(output);
        })
        .run(unit -> {
          new ServletServletResponse(unit.get(HttpServletRequest.class),
              unit.get(HttpServletResponse.class)).send(unit.get(FileChannel.class));
        });
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

}

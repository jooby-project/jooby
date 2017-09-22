package org.jooby.internal.jetty;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.HttpOutput;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.AsyncContext;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JettySse.class, Executors.class})
public class JettySseTest {

  private Block httpOutput = unit -> {
    Response rsp = unit.get(Response.class);
    expect(rsp.getHttpOutput()).andReturn(unit.get(HttpOutput.class));
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Request.class, Response.class, HttpOutput.class)
      .expect(httpOutput)
      .run(unit -> {
        new JettySse(unit.get(Request.class), unit.get(Response.class));
      });
  }

  @Test
  public void send() throws Exception {
    byte[] bytes = {0};
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit(Request.class, Response.class, HttpOutput.class)
      .expect(httpOutput)
      .expect(write(bytes))
      .run(unit -> {
        new JettySse(unit.get(Request.class),
          unit.get(Response.class))
          .send(Optional.of("1"), bytes).whenComplete((id, x) -> {
          if (x == null) {
            assertEquals("1", id.get());
            latch.countDown();
          }
        });
        latch.await();
      });
  }

  @Test
  public void sendFailure() throws Exception {
    byte[] bytes = {0};
    IOException cause = new IOException("intentional error");
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit(Request.class, Response.class, HttpOutput.class)
      .expect(httpOutput)
      .expect(unit -> {
        HttpOutput output = unit.get(HttpOutput.class);
        output.write(bytes);
        expectLastCall().andThrow(cause);
      })
      .run(unit -> {
        new JettySse(unit.get(Request.class),
          unit.get(Response.class))
          .send(Optional.of("1"), bytes).whenComplete((id, x) -> {
          if (x != null) {
            assertEquals(cause, x);
            latch.countDown();
          }
        });
        latch.await();
      });
  }

  @Test
  public void handshake() throws Exception {
    new MockUnit(Request.class, Response.class, HttpOutput.class, Runnable.class,
      AsyncContext.class, HttpChannel.class, Connector.class, Executor.class)
      .expect(httpOutput)
      .expect(unit -> {
        AsyncContext async = unit.get(AsyncContext.class);
        async.setTimeout(0L);

        Request req = unit.get(Request.class);
        expect(req.getAsyncContext()).andReturn(async);
      })
      .expect(unit -> {
        Response rsp = unit.get(Response.class);
        rsp.setStatus(200);
        rsp.setHeader("Connection", "Close");
        rsp.setContentType("text/event-stream; charset=utf-8");
        rsp.flushBuffer();

        HttpChannel channel = unit.get(HttpChannel.class);
        expect(rsp.getHttpChannel()).andReturn(channel);

        Connector connector = unit.get(Connector.class);
        expect(channel.getConnector()).andReturn(connector);

        Executor executor = unit.get(Executor.class);
        expect(connector.getExecutor()).andReturn(executor);

        executor.execute(unit.get(Runnable.class));
      })
      .run(unit -> {
        new JettySse(unit.get(Request.class), unit.get(Response.class))
          .handshake(unit.get(Runnable.class));
      });
  }

  @SuppressWarnings("resource")
  @Test
  public void shouldCloseEof() throws Exception {

    new MockUnit(Request.class, Response.class, HttpOutput.class)
      .expect(httpOutput)
      .run(unit -> {
        JettySse sse = new JettySse(unit.get(Request.class), unit.get(Response.class));
        assertEquals(true, sse.shouldClose(new EofException()));
      });
  }

  @SuppressWarnings("resource")
  @Test
  public void shouldCloseBrokenPipe() throws Exception {

    new MockUnit(Request.class, Response.class, HttpOutput.class)
      .expect(httpOutput)
      .run(unit -> {
        JettySse sse = new JettySse(unit.get(Request.class), unit.get(Response.class));
        assertEquals(true, sse.shouldClose(new IOException("broken pipe")));
      });
  }

  @Test
  public void close() throws Exception {
    new MockUnit(Request.class, Response.class, HttpOutput.class)
      .expect(httpOutput)
      .expect(unit -> {
        Response rsp = unit.get(Response.class);
        rsp.closeOutput();
      })
      .run(unit -> {
        JettySse sse = new JettySse(unit.get(Request.class), unit.get(Response.class));
        sse.close();
      });
  }

  @Test
  public void ignoreClosedStream() throws Exception {
    new MockUnit(Request.class, Response.class, HttpOutput.class)
      .expect(httpOutput)
      .expect(unit -> {
        Response rsp = unit.get(Response.class);
        rsp.closeOutput();
      })
      .run(unit -> {
        JettySse sse = new JettySse(unit.get(Request.class), unit.get(Response.class));
        sse.close();
        sse.close();
      });
  }

  @Test
  public void closeFailure() throws Exception {
    new MockUnit(Request.class, Response.class, HttpOutput.class)
      .expect(httpOutput)
      .expect(unit -> {
        Response rsp = unit.get(Response.class);
        rsp.closeOutput();
        expectLastCall().andThrow(new EofException("intentional err"));
      })
      .run(unit -> {
        JettySse sse = new JettySse(unit.get(Request.class), unit.get(Response.class));
        sse.close();
      });
  }

  private Block write(final byte[] bytes) {
    return unit -> {
      HttpOutput output = unit.get(HttpOutput.class);
      output.write(bytes);
      output.flush();
    };
  }

}

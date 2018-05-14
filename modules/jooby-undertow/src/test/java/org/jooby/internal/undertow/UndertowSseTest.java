package org.jooby.internal.undertow;

import io.undertow.io.IoCallback;
import io.undertow.io.Sender;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UndertowSse.class, HttpServerExchange.class, HeaderMap.class})
public class UndertowSseTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(HttpServerExchange.class)
      .run(unit -> {
        new UndertowSse(unit.get(HttpServerExchange.class));
      });
  }

  @Test
  public void close() throws Exception {
    new MockUnit(HttpServerExchange.class)
      .expect(unit -> {
        HttpServerExchange exchange = unit.get(HttpServerExchange.class);
        expect(exchange.endExchange()).andReturn(exchange);
      })
      .run(unit -> {
        new UndertowSse(unit.get(HttpServerExchange.class)).close();
      });
  }

  @Test
  public void handshake() throws Exception {
    new MockUnit(HttpServerExchange.class, Runnable.class, HeaderMap.class)
      .expect(unit -> {
        HeaderMap headers = unit.get(HeaderMap.class);
        expect(headers.put(Headers.CONNECTION, "Close")).andReturn(headers);
        expect(headers.put(Headers.CONTENT_TYPE, "text/event-stream; charset=utf-8"))
          .andReturn(headers);

        HttpServerExchange exchange = unit.get(HttpServerExchange.class);
        expect(exchange.getResponseHeaders()).andReturn(headers);
        expect(exchange.setStatusCode(200)).andReturn(exchange);
        expect(exchange.setPersistent(false)).andReturn(exchange);

        expect(exchange.dispatch(unit.get(Runnable.class))).andReturn(exchange);
      })
      .run(unit -> {
        new UndertowSse(unit.get(HttpServerExchange.class))
          .handshake(unit.get(Runnable.class));
      });
  }

  @Test
  public void send() throws Exception {
    byte[] bytes = {0};
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit(HttpServerExchange.class, Sender.class)
      .expect(unit -> {
        Sender sender = unit.get(Sender.class);
        sender.send(eq(ByteBuffer.wrap(bytes)), unit.capture(IoCallback.class));

        HttpServerExchange exchange = unit.get(HttpServerExchange.class);
        expect(exchange.getResponseSender()).andReturn(sender);
      })
      .run(unit -> {
        new UndertowSse(unit.get(HttpServerExchange.class))
          .send(Optional.of("id"), bytes).whenComplete((id, x) -> {
          if (id != null) {
            assertEquals("id", id.get());
            latch.countDown();
          }
        });
      }, unit -> {
        IoCallback callback = unit.captured(IoCallback.class).iterator().next();
        callback.onComplete(unit.get(HttpServerExchange.class), unit.get(Sender.class));
        latch.await();
      });
  }

  @Test(expected = IllegalStateException.class)
  public void sendError() throws Exception {
    byte[] bytes = {0};
    new MockUnit(HttpServerExchange.class)
      .expect(unit -> {
        HttpServerExchange exchange = unit.get(HttpServerExchange.class);
        expect(exchange.getResponseSender())
          .andThrow(new IllegalStateException("intentional err"));
      })
      .run(unit -> {
        new UndertowSse(unit.get(HttpServerExchange.class))
          .send(Optional.of("id"), bytes);
      });
  }

  @Test
  public void sendFailure() throws Exception {
    byte[] bytes = {0};
    IOException ex = new IOException("intentional err");
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit(HttpServerExchange.class, Sender.class)
      .expect(unit -> {
        Sender sender = unit.get(Sender.class);
        sender.send(eq(ByteBuffer.wrap(bytes)), unit.capture(IoCallback.class));

        HttpServerExchange exchange = unit.get(HttpServerExchange.class);
        expect(exchange.getResponseSender()).andReturn(sender);
      })
      .run(unit -> {
        new UndertowSse(unit.get(HttpServerExchange.class))
          .send(Optional.of("id"), bytes).whenComplete((id, cause) -> {
          if (cause != null) {
            assertEquals(ex, cause);
            latch.countDown();
          }
        });
      }, unit -> {
        IoCallback callback = unit.captured(IoCallback.class).iterator().next();
        callback.onException(unit.get(HttpServerExchange.class), unit.get(Sender.class), ex);
        latch.await();
      });
  }

}

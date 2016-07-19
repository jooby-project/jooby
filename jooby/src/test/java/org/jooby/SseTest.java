package org.jooby;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.jooby.internal.SseRenderer;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import javaslang.concurrent.Promise;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Sse.class, Deferred.class, Executors.class, SseRenderer.class })
public class SseTest {

  private Block handshake = unit -> {
    Request request = unit.get(Request.class);
    Injector injector = unit.get(Injector.class);
    Route route = unit.get(Route.class);
    Mutant lastEventId = unit.mock(Mutant.class);

    expect(route.produces()).andReturn(MediaType.ALL);

    expect(request.require(Injector.class)).andReturn(injector);
    expect(request.route()).andReturn(route);
    expect(request.attributes()).andReturn(ImmutableMap.of());
    expect(request.header("Last-Event-ID")).andReturn(lastEventId);

    expect(injector.getInstance(Renderer.KEY)).andReturn(Sets.newHashSet());
  };

  @Test
  public void sseId() throws Exception {
    Sse sse = new Sse() {

      @Override
      protected void closeInternal() {
      }

      @Override
      protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
        return null;
      }

      @Override
      protected void handshake(final Runnable handler) throws Exception {
      }
    };
    assertNotNull(sse.id());
    UUID.fromString(sse.id());
    sse.close();
  }

  @Test
  public void handshake() throws Exception {
    new MockUnit(Request.class, Injector.class, Runnable.class, Route.class)
        .expect(handshake)
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(Key.get(Object.class))).andReturn(null).times(2);
          expect(injector.getInstance(Key.get(TypeLiteral.get(Object.class)))).andReturn(null);
          expect(injector.getInstance(Key.get(Object.class, Names.named("n")))).andReturn(null);
        })
        .run(unit -> {
          Sse sse = new Sse() {

            @Override
            protected void closeInternal() {
            }

            @Override
            protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
              return null;
            }

            @Override
            protected void handshake(final Runnable handler) throws Exception {
            }
          };
          sse.handshake(unit.get(Request.class), unit.get(Runnable.class));
          sse.require(Object.class);
          sse.require(Key.get(Object.class));
          sse.require(TypeLiteral.get(Object.class));
          sse.require("n", Object.class);
          sse.close();
        });
  }

  @Test
  public void ifCloseClosedChannel() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit()
        .run(unit -> {
          Sse sse = new Sse() {

            @Override
            protected void closeInternal() {
              latch.countDown();
            }

            @Override
            protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
              return null;
            }

            @Override
            protected void handshake(final Runnable handler) throws Exception {
            }
          };
          sse.onClose(() -> sse.close());
          sse.ifClose(new ClosedChannelException());
          latch.await();
        });
  }

  @Test
  public void ifCloseBrokenPipe() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit()
        .run(unit -> {
          Sse sse = new Sse() {

            @Override
            protected void closeInternal() {
              latch.countDown();
            }

            @Override
            protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
              return null;
            }

            @Override
            protected void handshake(final Runnable handler) throws Exception {
            }
          };
          sse.onClose(() -> sse.close());
          sse.ifClose(new IOException("Broken pipe"));
          latch.await();
        });
  }

  @SuppressWarnings("resource")
  @Test
  public void ifCloseErrorOnFireClose() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit()
        .run(unit -> {
          Sse sse = new Sse() {

            @Override
            protected void closeInternal() {
              latch.countDown();
            }

            @Override
            protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
              return null;
            }

            @Override
            protected void handshake(final Runnable handler) throws Exception {
            }
          };
          sse.onClose(() -> {
            throw new IllegalStateException("intentional err");
          });
          sse.ifClose(new IOException("Broken pipe"));
          latch.await();
        });
  }

  @Test
  public void ifCloseFailure() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit()
        .run(unit -> {
          Sse sse = new Sse() {

            @Override
            protected void closeInternal() {
              latch.countDown();
            }

            @Override
            protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
              return null;
            }

            @Override
            protected void handshake(final Runnable handler) throws Exception {
            }
          };
          sse.onClose(() -> sse.close());
          sse.ifClose(new IOException("Broken pipe"));
          latch.await();
        });
  }

  @Test(expected = IllegalStateException.class)
  public void closeFailure() throws Exception {
    new MockUnit()
        .run(unit -> {
          Sse sse = new Sse() {

            @Override
            protected void closeInternal() {
              throw new IllegalStateException("intentional err");
            }

            @Override
            protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
              return null;
            }

            @Override
            protected void handshake(final Runnable handler) throws Exception {
            }
          };
          sse.close();
        });
  }

  @Test
  public void ifCloseIgnoreIO() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit()
        .run(unit -> {
          Sse sse = new Sse() {

            @Override
            protected void closeInternal() {
              latch.countDown();
            }

            @Override
            protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
              return null;
            }

            @Override
            protected void handshake(final Runnable handler) throws Exception {
            }
          };
          sse.onClose(() -> sse.close());
          sse.ifClose(new IOException("Ignored"));
          assertEquals(1, latch.getCount());
        });
  }

  @Test
  public void ifCloseIgnoreEx() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit()
        .run(unit -> {
          Sse sse = new Sse() {

            @Override
            protected void closeInternal() {
              latch.countDown();
            }

            @Override
            protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
              return null;
            }

            @Override
            protected void handshake(final Runnable handler) throws Exception {
            }
          };
          sse.onClose(() -> sse.close());
          sse.ifClose(new IllegalArgumentException("Ignored"));
          assertEquals(1, latch.getCount());
        });
  }

  @Test
  public void sseHandlerSuccess() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit(Request.class, Response.class, Route.Chain.class, Sse.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          Sse sse = unit.get(Sse.class);

          sse.handshake(eq(unit.get(Request.class)), unit.capture(Runnable.class));

          expect(req.require(Sse.class)).andReturn(sse);
          expect(req.path()).andReturn("/sse");
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.send(unit.capture(Deferred.class));
        })
        .run(unit -> {
          Sse.Handler handler = (req, sse) -> {
            latch.countDown();
          };
          handler.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        }, unit -> {
          Deferred deferred = unit.captured(Deferred.class).iterator().next();
          deferred.handler(null, (value, ex) -> {
          });

          unit.captured(Runnable.class).iterator().next().run();

          latch.await();
        });
  }

  @Test
  public void sseHandlerFailure() throws Exception {
    new MockUnit(Request.class, Response.class, Sse.class, Route.Chain.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          Sse sse = unit.get(Sse.class);

          sse.handshake(eq(unit.get(Request.class)), unit.capture(Runnable.class));

          expect(req.require(Sse.class)).andReturn(sse);
          expect(req.path()).andReturn("/sse");
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.send(unit.capture(Deferred.class));
        })
        .run(unit -> {
          Sse.Handler handler = (req, sse) -> {
            throw new IllegalStateException("intentional err");
          };
          handler.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        }, unit -> {
          Deferred deferred = unit.captured(Deferred.class).iterator().next();
          deferred.handler(null, (value, ex) -> {
          });

          unit.captured(Runnable.class).iterator().next().run();
        });
  }

  @Test
  public void sseHandlerHandshakeFailure() throws Exception {
    new MockUnit(Request.class, Response.class, Sse.class, Route.Chain.class)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          Sse sse = unit.get(Sse.class);

          sse.handshake(eq(unit.get(Request.class)), unit.capture(Runnable.class));
          expectLastCall().andThrow(new IllegalStateException("intentional error"));

          expect(req.require(Sse.class)).andReturn(sse);
          expect(req.path()).andReturn("/sse");
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          rsp.send(unit.capture(Deferred.class));
        })
        .run(unit -> {
          Sse.Handler handler = (req, sse) -> {
          };
          handler.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        }, unit -> {
          Deferred deferred = unit.captured(Deferred.class).iterator().next();
          deferred.handler(null, (value, ex) -> {
          });
        });
  }

  @Test
  public void sseKeepAlive() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit()
        .run(unit -> {
          Sse sse = new Sse() {

            @Override
            protected void closeInternal() {
            }

            @Override
            protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
              Promise<Optional<Object>> promise = Promise
                  .make(MoreExecutors.newDirectExecutorService());
              promise.success(id);
              return promise;
            }

            @Override
            public Sse keepAlive(final long millis) {
              assertEquals(100, millis);
              latch.countDown();
              return this;
            }

            @Override
            protected void handshake(final Runnable handler) throws Exception {
            }
          };

          new Sse.KeepAlive(sse, 100).run();
          latch.await();
        });
  }

  @SuppressWarnings("resource")
  @Test
  public void renderFailure() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    Object data = new Object();
    new MockUnit(Request.class, Route.class, Injector.class, Runnable.class)
        .expect(handshake)
        .expect(unit -> {
          SseRenderer renderer = unit.constructor(SseRenderer.class)
              .args(List.class, List.class, Charset.class, Map.class)
              .build(isA(List.class), isA(List.class), eq(StandardCharsets.UTF_8), isA(Map.class));

          expect(renderer.format(isA(Sse.Event.class))).andThrow(new IOException("failure"));
        })
        .run(unit -> {
          Sse sse = new Sse() {

            @Override
            protected void closeInternal() {
            }

            @Override
            protected void fireCloseEvent() {
            }

            @Override
            protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
              Promise<Optional<Object>> promise = Promise
                  .make(MoreExecutors.newDirectExecutorService());
              promise.failure(new IOException("intentional err"));
              return promise;
            }

            @Override
            public Sse keepAlive(final long millis) {
              return this;
            }

            @Override
            protected void handshake(final Runnable handler) throws Exception {
            }
          };
          sse.handshake(unit.get(Request.class), unit.get(Runnable.class));
          sse.event(data).type(MediaType.all).send().onFailure(cause -> latch.countDown());
          latch.await();
        });
  }

  @Test
  public void sseKeepAliveFailure() throws Exception {
    CountDownLatch latch = new CountDownLatch(2);
    new MockUnit()
        .run(unit -> {
          Sse sse = new Sse() {

            @Override
            protected void closeInternal() {
              latch.countDown();
            }

            @Override
            protected void fireCloseEvent() {
              latch.countDown();
            }

            @Override
            protected Promise<Optional<Object>> send(final Optional<Object> id, final byte[] data) {
              Promise<Optional<Object>> promise = Promise
                  .make(MoreExecutors.newDirectExecutorService());
              promise.failure(new IOException("intentional err"));
              return promise;
            }

            @Override
            public Sse keepAlive(final long millis) {
              return this;
            }

            @Override
            protected void handshake(final Runnable handler) throws Exception {
            }
          };

          new Sse.KeepAlive(sse, 100).run();
          latch.await();
        });
  }

}

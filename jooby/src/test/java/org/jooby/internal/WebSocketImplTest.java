package org.jooby.internal;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Renderer;
import org.jooby.Request;
import org.jooby.WebSocket;
import org.jooby.WebSocket.CloseStatus;
import org.jooby.WebSocket.OnClose;
import org.jooby.WebSocket.OnMessage;
import org.jooby.internal.parser.ParserExecutor;
import org.jooby.spi.NativeWebSocket;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WebSocketImpl.class, WebSocketRendererContext.class })
public class WebSocketImplTest {

  private Block connect = unit -> {
    WebSocket.OnOpen1 handler = unit.get(WebSocket.OnOpen1.class);
    handler.onOpen(eq(unit.get(Request.class)), isA(WebSocketImpl.class));

    Injector injector = unit.get(Injector.class);

    expect(injector.getInstance(Key.get(new TypeLiteral<Set<Renderer>>() {
    }))).andReturn(Collections.emptySet());

  };

  @SuppressWarnings("unchecked")
  private Block callbacks = unit -> {
    NativeWebSocket nws = unit.get(NativeWebSocket.class);
    nws.onBinaryMessage(isA(Consumer.class));
    nws.onTextMessage(isA(Consumer.class));
    nws.onErrorMessage(isA(Consumer.class));
    nws.onCloseMessage(isA(BiConsumer.class));
  };

  private Block locale = unit -> {
    Request req = unit.get(Request.class);
    expect(req.locale()).andReturn(Locale.CANADA);
  };

  @SuppressWarnings({"resource" })
  @Test
  public void sendString() throws Exception {
    Object data = "String";
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    new MockUnit(WebSocket.OnOpen1.class, WebSocket.SuccessCallback.class,
        WebSocket.OnError.class, Injector.class, Request.class, NativeWebSocket.class)
            .expect(connect)
            .expect(callbacks)
            .expect(locale)
            .expect(unit -> {
              List<Renderer> renderers = Collections.emptyList();

              NativeWebSocket ws = unit.get(NativeWebSocket.class);
              expect(ws.isOpen()).andReturn(true);

              WebSocketRendererContext ctx = unit.mockConstructor(WebSocketRendererContext.class,
                  new Class[]{List.class, NativeWebSocket.class, MediaType.class, Charset.class,
                      Locale.class,
                      WebSocket.SuccessCallback.class,
                      WebSocket.OnError.class },
                  renderers, ws,
                  produces, StandardCharsets.UTF_8,
                  Locale.CANADA,
                  unit.get(WebSocket.SuccessCallback.class),
                  unit.get(WebSocket.OnError.class));
              ctx.render(data);
            })
            .run(unit -> {
              WebSocketImpl ws = new WebSocketImpl(
                  unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
              ws.connect(unit.get(Injector.class), unit.get(Request.class),
                  unit.get(NativeWebSocket.class));

              ws.send(data, unit.get(WebSocket.SuccessCallback.class),
                  unit.get(WebSocket.OnError.class));
            });
  }

  @SuppressWarnings("unchecked")
  @Before
  @After
  public void resetSessions() throws Exception {
    Field field = WebSocketImpl.class.getDeclaredField("sessions");
    field.setAccessible(true);
    Map<String, List<WebSocket>> sessions = (Map<String, List<WebSocket>>) field.get(null);
    sessions.clear();
  }

  @SuppressWarnings({"resource" })
  @Test
  public void sendBroadcast() throws Exception {
    Object data = "String";
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    new MockUnit(WebSocket.OnOpen1.class, WebSocket.SuccessCallback.class,
        WebSocket.OnError.class, Injector.class, Request.class, NativeWebSocket.class)
            .expect(connect)
            .expect(callbacks)
            .expect(locale)
            .expect(unit -> {
              List<Renderer> renderers = Collections.emptyList();

              NativeWebSocket ws = unit.get(NativeWebSocket.class);
              expect(ws.isOpen()).andReturn(true);

              WebSocketRendererContext ctx = unit.mockConstructor(WebSocketRendererContext.class,
                  new Class[]{List.class, NativeWebSocket.class, MediaType.class, Charset.class,
                      Locale.class,
                      WebSocket.SuccessCallback.class,
                      WebSocket.OnError.class },
                  renderers, ws,
                  produces, StandardCharsets.UTF_8,
                  Locale.CANADA,
                  unit.get(WebSocket.SuccessCallback.class),
                  unit.get(WebSocket.OnError.class));
              ctx.render(data);
            })
            .run(unit -> {
              WebSocketImpl ws = new WebSocketImpl(
                  unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
              ws.connect(unit.get(Injector.class), unit.get(Request.class),
                  unit.get(NativeWebSocket.class));

              ws.broadcast(data, unit.get(WebSocket.SuccessCallback.class),
                  unit.get(WebSocket.OnError.class));
            });
  }

  @SuppressWarnings({"resource" })
  @Test
  public void sendBroadcastErr() throws Exception {
    Object data = "String";
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    new MockUnit(WebSocket.OnOpen1.class, WebSocket.SuccessCallback.class,
        WebSocket.OnError.class, Injector.class, Request.class, NativeWebSocket.class)
            .expect(connect)
            .expect(callbacks)
            .expect(locale)
            .expect(unit -> {
              List<Renderer> renderers = Collections.emptyList();

              NativeWebSocket ws = unit.get(NativeWebSocket.class);
              expect(ws.isOpen()).andReturn(true);

              WebSocketRendererContext ctx = unit.mockConstructor(WebSocketRendererContext.class,
                  new Class[]{List.class, NativeWebSocket.class, MediaType.class, Charset.class,
                      Locale.class,
                      WebSocket.SuccessCallback.class,
                      WebSocket.OnError.class },
                  renderers, ws,
                  produces, StandardCharsets.UTF_8,
                  Locale.CANADA,
                  unit.get(WebSocket.SuccessCallback.class),
                  unit.get(WebSocket.OnError.class));
              ctx.render(data);
              IllegalStateException x = new IllegalStateException("intentional err");
              expectLastCall().andThrow(x);
              unit.get(WebSocket.OnError.class).onError(x);
            })
            .run(unit -> {
              WebSocketImpl ws = new WebSocketImpl(
                  unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
              ws.connect(unit.get(Injector.class), unit.get(Request.class),
                  unit.get(NativeWebSocket.class));

              ws.broadcast(data, unit.get(WebSocket.SuccessCallback.class),
                  unit.get(WebSocket.OnError.class));
            });
  }

  @SuppressWarnings({"resource" })
  @Test(expected = Err.class)
  public void sendClose() throws Exception {
    Object data = "String";
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    new MockUnit(WebSocket.OnOpen1.class, WebSocket.SuccessCallback.class,
        WebSocket.OnError.class, Injector.class, Request.class, NativeWebSocket.class)
            .expect(connect)
            .expect(callbacks)
            .expect(locale)
            .expect(unit -> {
              NativeWebSocket ws = unit.get(NativeWebSocket.class);
              expect(ws.isOpen()).andReturn(false);
            })
            .run(unit -> {
              WebSocketImpl ws = new WebSocketImpl(
                  unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
              ws.connect(unit.get(Injector.class), unit.get(Request.class),
                  unit.get(NativeWebSocket.class));

              ws.send(data, unit.get(WebSocket.SuccessCallback.class),
                  unit.get(WebSocket.OnError.class));
            });
  }

  @SuppressWarnings("resource")
  @Test
  public void toStr() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;

    new MockUnit(WebSocket.OnOpen1.class)
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
          assertEquals("WS /\n" +
              "  pattern: /pattern\n" +
              "  vars: {}\n" +
              "  consumes: */*\n" +
              "  produces: */*\n" +
              "", ws.toString());
        });
  }

  @SuppressWarnings({"resource" })
  @Test
  public void isOpen() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    new MockUnit(WebSocket.OnOpen1.class, WebSocket.SuccessCallback.class,
        WebSocket.OnError.class, Injector.class, Request.class, NativeWebSocket.class)
            .expect(connect)
            .expect(callbacks)
            .expect(locale)
            .expect(unit -> {
              NativeWebSocket ws = unit.get(NativeWebSocket.class);
              expect(ws.isOpen()).andReturn(true);
            })
            .run(unit -> {
              WebSocketImpl ws = new WebSocketImpl(
                  unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
              ws.connect(unit.get(Injector.class), unit.get(Request.class),
                  unit.get(NativeWebSocket.class));

              assertTrue(ws.isOpen());
            });
  }

  @SuppressWarnings("resource")
  @Test
  public void pauseAndResume() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;

    new MockUnit(WebSocket.OnOpen1.class, Injector.class, Request.class, NativeWebSocket.class)
        .expect(connect)
        .expect(callbacks)
        .expect(locale)
        .expect(unit -> {
          NativeWebSocket channel = unit.get(NativeWebSocket.class);
          channel.pause();

          channel.resume();
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
          ws.connect(unit.get(Injector.class), unit.get(Request.class),
              unit.get(NativeWebSocket.class));
          ws.pause();

          ws.pause();

          ws.resume();

          ws.resume();
        });
  }

  @Test
  public void close() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;

    new MockUnit(WebSocket.OnOpen1.class, Injector.class, Request.class, NativeWebSocket.class)
        .expect(connect)
        .expect(callbacks)
        .expect(locale)
        .expect(unit -> {
          NativeWebSocket ws = unit.get(NativeWebSocket.class);
          ws.close(WebSocket.NORMAL.code(), WebSocket.NORMAL.reason());
        }).run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
          ws.connect(unit.get(Injector.class), unit.get(Request.class),
              unit.get(NativeWebSocket.class));
          ws.close(WebSocket.NORMAL);
        });
  }

  @SuppressWarnings("resource")
  @Test
  public void terminate() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;

    new MockUnit(WebSocket.OnOpen1.class, Injector.class, Request.class, NativeWebSocket.class)
        .expect(connect)
        .expect(callbacks)
        .expect(locale)
        .expect(unit -> {
          NativeWebSocket ws = unit.get(NativeWebSocket.class);
          ws.terminate();
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
          ws.connect(unit.get(Injector.class), unit.get(Request.class),
              unit.get(NativeWebSocket.class));
          ws.terminate();
        });
  }

  @SuppressWarnings("resource")
  @Test
  public void props() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;

    new MockUnit(WebSocket.OnOpen1.class)
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
          assertEquals(pattern, ws.pattern());
          assertEquals(path, ws.path());
          assertEquals(consumes, ws.consumes());
          assertEquals(produces, ws.produces());
        });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void require() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    Object instance = new Object();

    new MockUnit(WebSocket.OnOpen1.class, Injector.class, Request.class, NativeWebSocket.class)
        .expect(connect)
        .expect(locale)
        .expect(unit -> {
          NativeWebSocket nws = unit.get(NativeWebSocket.class);
          nws.onBinaryMessage(isA(Consumer.class));
          nws.onTextMessage(isA(Consumer.class));
          nws.onErrorMessage(isA(Consumer.class));
          nws.onCloseMessage(isA(BiConsumer.class));
        })
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(Key.get(Object.class))).andReturn(instance);
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
          ws.connect(unit.get(Injector.class), unit.get(Request.class),
              unit.get(NativeWebSocket.class));
          assertEquals(instance, ws.require(Object.class));
        });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void onMessage() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;

    new MockUnit(WebSocket.OnOpen1.class, Injector.class, OnMessage.class, Request.class,
        NativeWebSocket.class,
        Mutant.class)
            .expect(connect)
            .expect(locale)
            .expect(unit -> {
              NativeWebSocket nws = unit.get(NativeWebSocket.class);
              nws.onBinaryMessage(isA(Consumer.class));
              nws.onTextMessage(unit.capture(Consumer.class));
              nws.onErrorMessage(isA(Consumer.class));
              nws.onCloseMessage(isA(BiConsumer.class));
            })
            .expect(unit -> {
              OnMessage<Mutant> callback = unit.get(OnMessage.class);
              callback.onMessage(isA(Mutant.class));
            })
            .expect(unit -> {
              Injector injector = unit.get(Injector.class);
              expect(injector.getInstance(ParserExecutor.class)).andReturn(
                  unit.mock(ParserExecutor.class));
            })
            .run(unit -> {
              WebSocketImpl ws = new WebSocketImpl(
                  unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
              ws.connect(unit.get(Injector.class), unit.get(Request.class),
                  unit.get(NativeWebSocket.class));
              ws.onMessage(unit.get(OnMessage.class));
            }, unit -> {
              unit.captured(Consumer.class).iterator().next().accept("something");
            });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void onErr() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    Exception ex = new Exception();

    new MockUnit(WebSocket.OnOpen1.class, Injector.class, Request.class, NativeWebSocket.class,
        WebSocket.OnError.class)
            .expect(connect)
            .expect(locale)
            .expect(unit -> {
              NativeWebSocket nws = unit.get(NativeWebSocket.class);
              nws.onBinaryMessage(isA(Consumer.class));
              nws.onTextMessage(isA(Consumer.class));
              nws.onErrorMessage(unit.capture(Consumer.class));
              nws.onCloseMessage(isA(BiConsumer.class));

              expect(nws.isOpen()).andReturn(false);
            })
            .expect(unit -> {
              WebSocket.OnError callback = unit.get(WebSocket.OnError.class);
              callback.onError(ex);
            })
            .run(unit -> {
              WebSocketImpl ws = new WebSocketImpl(
                  unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
              ws.connect(unit.get(Injector.class), unit.get(Request.class),
                  unit.get(NativeWebSocket.class));
              ws.onError(unit.get(WebSocket.OnError.class));
            }, unit -> {
              unit.captured(Consumer.class).iterator().next().accept(ex);
            });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void onSilentErr() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    Exception ex = new ClosedChannelException();

    new MockUnit(WebSocket.OnOpen1.class, Injector.class, Request.class, NativeWebSocket.class,
        WebSocket.OnError.class)
            .expect(connect)
            .expect(locale)
            .expect(unit -> {
              NativeWebSocket nws = unit.get(NativeWebSocket.class);
              nws.onBinaryMessage(isA(Consumer.class));
              nws.onTextMessage(isA(Consumer.class));
              nws.onErrorMessage(unit.capture(Consumer.class));
              nws.onCloseMessage(isA(BiConsumer.class));

              expect(nws.isOpen()).andReturn(false);
            })
            .run(unit -> {
              WebSocketImpl ws = new WebSocketImpl(
                  unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
              ws.connect(unit.get(Injector.class), unit.get(Request.class),
                  unit.get(NativeWebSocket.class));
              ws.onError(unit.get(WebSocket.OnError.class));
            }, unit -> {
              unit.captured(Consumer.class).iterator().next().accept(ex);
            });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void onErrAndWsOpen() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    Exception ex = new Exception();

    new MockUnit(WebSocket.OnOpen1.class, Injector.class, Request.class, NativeWebSocket.class,
        WebSocket.OnError.class)
            .expect(connect)
            .expect(locale)
            .expect(unit -> {
              NativeWebSocket nws = unit.get(NativeWebSocket.class);
              nws.onBinaryMessage(isA(Consumer.class));
              nws.onTextMessage(isA(Consumer.class));
              nws.onErrorMessage(unit.capture(Consumer.class));
              nws.onCloseMessage(isA(BiConsumer.class));

              expect(nws.isOpen()).andReturn(true);
              nws.close(1011, "Server error");
            })
            .expect(unit -> {
              WebSocket.OnError callback = unit.get(WebSocket.OnError.class);
              callback.onError(ex);
            })
            .run(unit -> {
              WebSocketImpl ws = new WebSocketImpl(
                  unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
              ws.connect(unit.get(Injector.class), unit.get(Request.class),
                  unit.get(NativeWebSocket.class));
              ws.onError(unit.get(WebSocket.OnError.class));
            }, unit -> {
              unit.captured(Consumer.class).iterator().next().accept(ex);
            });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void onClose() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    WebSocket.CloseStatus status = WebSocket.NORMAL;

    new MockUnit(WebSocket.OnOpen1.class, OnMessage.class, OnClose.class, Request.class,
        NativeWebSocket.class, Injector.class)
            .expect(connect)
            .expect(locale)
            .expect(unit -> {
              NativeWebSocket nws = unit.get(NativeWebSocket.class);
              nws.onBinaryMessage(isA(Consumer.class));
              nws.onTextMessage(isA(Consumer.class));
              nws.onErrorMessage(isA(Consumer.class));
              nws.onCloseMessage(unit.capture(BiConsumer.class));
            })
            .expect(unit -> {
              OnClose callback = unit.get(OnClose.class);
              callback.onClose(unit.capture(WebSocket.CloseStatus.class));
            })
            .run(unit -> {
              WebSocketImpl ws = new WebSocketImpl(
                  unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
              ws.connect(unit.get(Injector.class), unit.get(Request.class),
                  unit.get(NativeWebSocket.class));
              ws.onClose(unit.get(WebSocket.OnClose.class));
            }, unit -> {
              unit.captured(BiConsumer.class).iterator().next()
                  .accept(status.code(), Optional.of(status.reason()));
            }, unit -> {
              CloseStatus captured = unit.captured(WebSocket.CloseStatus.class).iterator().next();
              assertEquals(status.code(), captured.code());
              assertEquals(status.reason(), captured.reason());
            });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void onCloseNullReason() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    WebSocket.CloseStatus status = WebSocket.CloseStatus.of(1000);

    new MockUnit(WebSocket.OnOpen1.class, OnMessage.class, OnClose.class, NativeWebSocket.class,
        Request.class, Injector.class)
            .expect(connect)
            .expect(locale)
            .expect(unit -> {
              NativeWebSocket nws = unit.get(NativeWebSocket.class);
              nws.onBinaryMessage(isA(Consumer.class));
              nws.onTextMessage(isA(Consumer.class));
              nws.onErrorMessage(isA(Consumer.class));
              nws.onCloseMessage(unit.capture(BiConsumer.class));
            })
            .expect(unit -> {
              OnClose callback = unit.get(OnClose.class);
              callback.onClose(unit.capture(WebSocket.CloseStatus.class));
            })
            .run(unit -> {
              WebSocketImpl ws = new WebSocketImpl(
                  unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
              ws.connect(unit.get(Injector.class), unit.get(Request.class),
                  unit.get(NativeWebSocket.class));
              ws.onClose(unit.get(OnClose.class));
            }, unit -> {
              unit.captured(BiConsumer.class).iterator().next()
                  .accept(status.code(), Optional.empty());
            }, unit -> {
              CloseStatus captured = unit.captured(WebSocket.CloseStatus.class).iterator().next();
              assertEquals(status.code(), captured.code());
              assertEquals(null, captured.reason());
            });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void onCloseEmptyReason() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<Object, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    WebSocket.CloseStatus status = WebSocket.CloseStatus.of(1000, "");

    new MockUnit(WebSocket.OnOpen1.class, OnMessage.class, NativeWebSocket.class, Request.class,
        Injector.class, OnClose.class)
            .expect(connect)
            .expect(locale)
            .expect(unit -> {
              NativeWebSocket nws = unit.get(NativeWebSocket.class);
              nws.onBinaryMessage(isA(Consumer.class));
              nws.onTextMessage(isA(Consumer.class));
              nws.onErrorMessage(isA(Consumer.class));
              nws.onCloseMessage(unit.capture(BiConsumer.class));
            })
            .expect(unit -> {
              OnClose callback = unit.get(OnClose.class);
              callback.onClose(unit.capture(WebSocket.CloseStatus.class));
            })
            .run(unit -> {
              WebSocketImpl ws = new WebSocketImpl(
                  unit.get(WebSocket.OnOpen1.class), path, pattern, vars, consumes, produces);
              ws.connect(unit.get(Injector.class), unit.get(Request.class),
                  unit.get(NativeWebSocket.class));
              ws.onClose(unit.get(OnClose.class));
            }, unit -> {
              unit.captured(BiConsumer.class).iterator().next()
                  .accept(status.code(), Optional.of(""));
            }, unit -> {
              CloseStatus captured = unit.captured(WebSocket.CloseStatus.class).iterator().next();
              assertEquals(status.code(), captured.code());
              assertEquals(null, captured.reason());
            });
  }

}

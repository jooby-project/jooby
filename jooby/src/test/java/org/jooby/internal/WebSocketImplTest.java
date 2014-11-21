package org.jooby.internal;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.SuspendToken;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.jooby.Body;
import org.jooby.MediaType;
import org.jooby.MockUnit;
import org.jooby.MockUnit.Block;
import org.jooby.Mutant;
import org.jooby.WebSocket;
import org.jooby.WebSocket.Callback;
import org.junit.Test;

import com.google.inject.Injector;
import com.google.inject.Key;

public class WebSocketImplTest {

  private Block connect = unit -> {
    WebSocket.Handler handler = unit.get(WebSocket.Handler.class);
    handler.connect(isA(WebSocketImpl.class));
  };

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void sendString() throws Exception {
    Object data = "String";
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    Exception cause = new Exception();

    new MockUnit(WebSocket.Handler.class, WebSocket.Callback0.class, WebSocket.Callback.class,
        Injector.class, Session.class)
        .expect(connect)
        .expect(unit -> {
          RemoteEndpoint remote = unit.mock(RemoteEndpoint.class);
          remote.sendString((String) eq(data), unit.capture(WriteCallback.class));

          Session session = unit.get(Session.class);
          expect(session.getRemote()).andReturn(remote);
        })
        .expect(unit -> {
          BodyConverterSelector selector = unit.mock(BodyConverterSelector.class);
          expect(selector.forWrite(data, Arrays.asList(produces))).andReturn(Optional.empty());

          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(BodyConverterSelector.class)).andReturn(selector);
        })
        .expect(unit -> {
          WebSocket.Callback0 success = unit.get(WebSocket.Callback0.class);
          success.invoke();

          WebSocket.Callback<Exception> error = unit.get(WebSocket.Callback.class);
          error.invoke(cause);
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.connect(unit.get(Injector.class), unit.get(Session.class));

          ws.send(data, unit.get(WebSocket.Callback0.class), unit.get(WebSocket.Callback.class));
        }, unit -> {
          WriteCallback callback = unit.captured(WriteCallback.class).iterator().next();
          callback.writeSuccess();
          callback.writeFailed(cause);
        });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void sendCallbacksWithErrors() throws Exception {
    Object data = "String";
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    Exception cause = new Exception();

    new MockUnit(WebSocket.Handler.class, WebSocket.Callback0.class, WebSocket.Callback.class,
        Injector.class, Session.class)
        .expect(connect)
        .expect(unit -> {
          RemoteEndpoint remote = unit.mock(RemoteEndpoint.class);
          remote.sendString((String) eq(data), unit.capture(WriteCallback.class));

          Session session = unit.get(Session.class);
          expect(session.getRemote()).andReturn(remote);
        })
        .expect(unit -> {
          BodyConverterSelector selector = unit.mock(BodyConverterSelector.class);
          expect(selector.forWrite(data, Arrays.asList(produces))).andReturn(Optional.empty());

          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(BodyConverterSelector.class)).andReturn(selector);
        })
        .expect(unit -> {
          WebSocket.Callback0 success = unit.get(WebSocket.Callback0.class);
          success.invoke();
          expectLastCall().andThrow(new Exception());

          WebSocket.Callback<Exception> error = unit.get(WebSocket.Callback.class);
          error.invoke(cause);
          expectLastCall().andThrow(new Exception());
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.connect(unit.get(Injector.class), unit.get(Session.class));

          ws.send(data, unit.get(WebSocket.Callback0.class), unit.get(WebSocket.Callback.class));
        }, unit -> {
          WriteCallback callback = unit.captured(WriteCallback.class).iterator().next();
          callback.writeSuccess();
          callback.writeFailed(cause);
        });

  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void sendBytes() throws Exception {
    byte[] data = "String".getBytes();
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    Exception cause = new Exception();

    new MockUnit(WebSocket.Handler.class, WebSocket.Callback0.class, WebSocket.Callback.class,
        Injector.class, Session.class)
        .expect(connect)
        .expect(unit -> {
          RemoteEndpoint remote = unit.mock(RemoteEndpoint.class);
          remote.sendBytes(unit.capture(ByteBuffer.class), unit.capture(WriteCallback.class));

          Session session = unit.get(Session.class);
          expect(session.getRemote()).andReturn(remote);
        })
        .expect(unit -> {
          BodyConverterSelector selector = unit.mock(BodyConverterSelector.class);
          expect(selector.forWrite(data, Arrays.asList(produces))).andReturn(Optional.empty());

          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(BodyConverterSelector.class)).andReturn(selector);
        })
        .expect(unit -> {
          WebSocket.Callback0 success = unit.get(WebSocket.Callback0.class);
          success.invoke();

          WebSocket.Callback<Exception> error = unit.get(WebSocket.Callback.class);
          error.invoke(cause);
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.connect(unit.get(Injector.class), unit.get(Session.class));

          ws.send(data, unit.get(WebSocket.Callback0.class), unit.get(WebSocket.Callback.class));
        }, unit -> {
          WriteCallback callback = unit.captured(WriteCallback.class).iterator().next();
          callback.writeSuccess();
          callback.writeFailed(cause);
        }, unit -> {
          ByteBuffer buffer = unit.captured(ByteBuffer.class).iterator().next();
          assertArrayEquals(data, buffer.array());
        });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void sendByteBuffer() throws Exception {
    ByteBuffer data = ByteBuffer.wrap(new byte[0]);
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    Exception cause = new Exception();

    new MockUnit(WebSocket.Handler.class, WebSocket.Callback0.class, WebSocket.Callback.class,
        Injector.class, Session.class)
        .expect(connect)
        .expect(unit -> {
          RemoteEndpoint remote = unit.mock(RemoteEndpoint.class);
          remote.sendBytes(eq(data), unit.capture(WriteCallback.class));

          Session session = unit.get(Session.class);
          expect(session.getRemote()).andReturn(remote);
        })
        .expect(unit -> {
          BodyConverterSelector selector = unit.mock(BodyConverterSelector.class);
          expect(selector.forWrite(data, Arrays.asList(produces))).andReturn(Optional.empty());

          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(BodyConverterSelector.class)).andReturn(selector);
        })
        .expect(unit -> {
          WebSocket.Callback0 success = unit.get(WebSocket.Callback0.class);
          success.invoke();

          WebSocket.Callback<Exception> error = unit.get(WebSocket.Callback.class);
          error.invoke(cause);
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.connect(unit.get(Injector.class), unit.get(Session.class));

          ws.send(data, unit.get(WebSocket.Callback0.class), unit.get(WebSocket.Callback.class));
        }, unit -> {
          WriteCallback callback = unit.captured(WriteCallback.class).iterator().next();
          callback.writeSuccess();
          callback.writeFailed(cause);
        });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void sendTextFormat() throws Exception {
    Map<String, Object> data = new HashMap<>();
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    Exception cause = new Exception();

    new MockUnit(WebSocket.Handler.class, WebSocket.Callback0.class, WebSocket.Callback.class,
        Injector.class, Session.class)
        .expect(connect)
        .expect(unit -> {
          RemoteEndpoint remote = unit.mock(RemoteEndpoint.class);
          remote.sendString(eq("{}"), unit.capture(WriteCallback.class));

          Session session = unit.get(Session.class);
          expect(session.getRemote()).andReturn(remote);
        })
        .expect(
            unit -> {
              Body.Formatter formatter = unit.mock(Body.Formatter.class);
              formatter.format(eq(data), unit.capture(Body.Writer.class));

              BodyConverterSelector selector = unit.mock(BodyConverterSelector.class);
              expect(selector.forWrite(data, Arrays.asList(produces))).andReturn(
                  Optional.of(formatter));

              Injector injector = unit.get(Injector.class);
              expect(injector.getInstance(BodyConverterSelector.class)).andReturn(selector);
            })
        .expect(unit -> {
          WebSocket.Callback0 success = unit.get(WebSocket.Callback0.class);
          success.invoke();

          WebSocket.Callback<Exception> error = unit.get(WebSocket.Callback.class);
          error.invoke(cause);
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.connect(unit.get(Injector.class), unit.get(Session.class));

          ws.send(data, unit.get(WebSocket.Callback0.class), unit.get(WebSocket.Callback.class));
        }, unit -> {
          Body.Writer writer = unit.captured(Body.Writer.class).iterator().next();
          writer.text((w) -> {
            w.write("{}");
          });
        }, unit -> {
          WriteCallback callback = unit.captured(WriteCallback.class).iterator().next();
          callback.writeSuccess();
          callback.writeFailed(cause);
        });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void sendBinFormat() throws Exception {
    Map<String, Object> data = new HashMap<>();
    byte[] bytes = new byte[0];
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    Exception cause = new Exception();

    new MockUnit(WebSocket.Handler.class, WebSocket.Callback0.class, WebSocket.Callback.class,
        Injector.class, Session.class)
        .expect(connect)
        .expect(unit -> {
          RemoteEndpoint remote = unit.mock(RemoteEndpoint.class);
          remote.sendBytes(unit.capture(ByteBuffer.class), unit.capture(WriteCallback.class));

          Session session = unit.get(Session.class);
          expect(session.getRemote()).andReturn(remote);
        })
        .expect(
            unit -> {
              Body.Formatter formatter = unit.mock(Body.Formatter.class);
              formatter.format(eq(data), unit.capture(Body.Writer.class));

              BodyConverterSelector selector = unit.mock(BodyConverterSelector.class);
              expect(selector.forWrite(data, Arrays.asList(produces))).andReturn(
                  Optional.of(formatter));

              Injector injector = unit.get(Injector.class);
              expect(injector.getInstance(BodyConverterSelector.class)).andReturn(selector);
            })
        .expect(unit -> {
          WebSocket.Callback0 success = unit.get(WebSocket.Callback0.class);
          success.invoke();

          WebSocket.Callback<Exception> error = unit.get(WebSocket.Callback.class);
          error.invoke(cause);
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.connect(unit.get(Injector.class), unit.get(Session.class));

          ws.send(data, unit.get(WebSocket.Callback0.class), unit.get(WebSocket.Callback.class));
        }, unit -> {
          Body.Writer writer = unit.captured(Body.Writer.class).iterator().next();
          writer.bytes((stream) -> {
            stream.write(bytes);
          });
        }, unit -> {
          WriteCallback callback = unit.captured(WriteCallback.class).iterator().next();
          callback.writeSuccess();
          callback.writeFailed(cause);
        }, unit -> {
          ByteBuffer buffer = unit.captured(ByteBuffer.class).iterator().next();
          assertArrayEquals(bytes, buffer.array());
        });
  }

  @SuppressWarnings("resource")
  @Test
  public void toStr() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;

    new MockUnit(WebSocket.Handler.class)
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          assertEquals("WS /\n" +
              "  pattern: /pattern\n" +
              "  vars: {}\n" +
              "  consumes: */*\n" +
              "  produces: */*\n" +
              "", ws.toString());
        });
  }

  @SuppressWarnings("resource")
  @Test
  public void pauseAndResume() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;

    new MockUnit(WebSocket.Handler.class, Injector.class, Session.class, SuspendToken.class)
        .expect(connect)
        .expect(unit -> {
          SuspendToken token = unit.get(SuspendToken.class);
          token.resume();

          Session session = unit.get(Session.class);
          expect(session.suspend()).andReturn(token);
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.connect(unit.get(Injector.class), unit.get(Session.class));
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
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;

    new MockUnit(WebSocket.Handler.class, Injector.class, Session.class)
        .expect(connect)
        .expect(unit -> {

          Session session = unit.get(Session.class);
          session.close(1000, "Normal");
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.connect(unit.get(Injector.class), unit.get(Session.class));
          ws.close(WebSocket.NORMAL);
        });
  }

  @SuppressWarnings("resource")
  @Test
  public void terminate() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;

    new MockUnit(WebSocket.Handler.class, Injector.class, Session.class)
        .expect(connect)
        .expect(unit -> {

          Session session = unit.get(Session.class);
          session.disconnect();
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.connect(unit.get(Injector.class), unit.get(Session.class));
          ws.terminate();
        });
  }

  @SuppressWarnings("resource")
  @Test
  public void isOpen() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;

    new MockUnit(WebSocket.Handler.class, Injector.class, Session.class)
        .expect(connect)
        .expect(unit -> {

          Session session = unit.get(Session.class);
          expect(session.isOpen()).andReturn(true);
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.connect(unit.get(Injector.class), unit.get(Session.class));
          assertEquals(true, ws.isOpen());
        });
  }

  @SuppressWarnings("resource")
  @Test
  public void props() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;

    new MockUnit(WebSocket.Handler.class)
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          assertEquals(pattern, ws.pattern());
          assertEquals(path, ws.path());
          assertEquals(vars, ws.vars());
          assertEquals(consumes, ws.consumes());
          assertEquals(produces, ws.produces());
        });
  }

  @SuppressWarnings("resource")
  @Test
  public void getInstance() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    Object instance = new Object();

    new MockUnit(WebSocket.Handler.class, Injector.class, Session.class, SuspendToken.class)
        .expect(connect)
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(Key.get(Object.class))).andReturn(instance);
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.connect(unit.get(Injector.class), unit.get(Session.class));
          assertEquals(instance, ws.getInstance(Object.class));
        });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void onMessage() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;

    new MockUnit(WebSocket.Handler.class, Callback.class, Mutant.class)
        .expect(unit -> {
          Callback<Mutant> callback = unit.get(Callback.class);
          callback.invoke(unit.get(Mutant.class));
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.onMessage(unit.get(Callback.class));
          ws.fireMessage(unit.get(Mutant.class));;
        });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void onErr() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    Exception ex = new Exception();

    new MockUnit(WebSocket.Handler.class, Callback.class)
        .expect(unit -> {
          Callback<Exception> callback = unit.get(Callback.class);
          callback.invoke(ex);
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.onError(unit.get(Callback.class));
          ws.fireErr(ex);;
        });
  }

  @SuppressWarnings({"resource", "unchecked" })
  @Test
  public void onClose() throws Exception {
    String path = "/";
    String pattern = "/pattern";
    Map<String, String> vars = new HashMap<>();
    MediaType consumes = MediaType.all;
    MediaType produces = MediaType.all;
    WebSocket.CloseStatus status = WebSocket.NORMAL;

    new MockUnit(WebSocket.Handler.class, Callback.class)
        .expect(unit -> {
          Callback<WebSocket.CloseStatus> callback = unit.get(Callback.class);
          callback.invoke(status);
        })
        .run(unit -> {
          WebSocketImpl ws = new WebSocketImpl(
              unit.get(WebSocket.Handler.class), path, pattern, vars, consumes, produces
              );
          ws.onClose(unit.get(Callback.class));
          ws.fireClose(status);;
        });
  }

}

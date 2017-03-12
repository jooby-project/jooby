package org.jooby.internal.mvc;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.List;

import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.WebSocket;
import org.jooby.WebSocket.CloseStatus;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.util.Types;

public class MvcWebSocketTest {

  public static class ImNotACallback {
  }

  @SuppressWarnings("rawtypes")
  public static class CallbackWithoutType implements WebSocket.OnMessage {
    @Override
    public void onMessage(final Object message) throws Exception {
    }
  }

  public static class StringSocket implements WebSocket.OnMessage<String> {
    @Override
    public void onMessage(final String message) throws Exception {
    }
  }

  public static class Pojo {

  }

  public static class PojoSocket implements WebSocket.OnMessage<Pojo> {
    @Override
    public void onMessage(final Pojo message) throws Exception {
    }
  }

  public static class PojoListSocket implements WebSocket.OnMessage<List<Pojo>> {
    @Override
    public void onMessage(final List<Pojo> message) throws Exception {
    }
  }

  public static class MySocket
      implements WebSocket.OnMessage<String>, WebSocket.OnClose, WebSocket.OnError,
      WebSocket.OnOpen {

    @Override
    public void onMessage(final String data) throws Exception {
    }

    @Override
    public void onOpen(final Request req, final WebSocket ws) throws Exception {
    }

    @Override
    public void onError(final Throwable err) {
    }

    @Override
    public void onClose(final CloseStatus status) throws Exception {
    }

  }

  @Test
  public void newInstance() throws Exception {
    new MockUnit(WebSocket.class, Injector.class, MySocket.class, Binder.class)
        .expect(childInjector(MySocket.class))
        .run(unit -> {
          new MvcWebSocket(unit.get(WebSocket.class), MySocket.class);
        }, unit -> {
          unit.captured(Module.class).iterator().next().configure(unit.get(Binder.class));
        });
  }

  @Test
  public void onClose() throws Exception {
    new MockUnit(WebSocket.class, Injector.class, MySocket.class, Binder.class)
        .expect(childInjector(MySocket.class))
        .expect(unit -> {
          unit.get(MySocket.class).onClose(WebSocket.NORMAL);
        })
        .run(unit -> {
          new MvcWebSocket(unit.get(WebSocket.class), MySocket.class).onClose(WebSocket.NORMAL);
        }, unit -> {
          unit.captured(Module.class).iterator().next().configure(unit.get(Binder.class));
        });
  }

  @Test
  public void shouldIgnoreOnClose() throws Exception {
    new MockUnit(WebSocket.class, Injector.class, StringSocket.class, Binder.class)
        .expect(childInjector(StringSocket.class))
        .run(unit -> {
          new MvcWebSocket(unit.get(WebSocket.class), StringSocket.class).onClose(WebSocket.NORMAL);
        }, unit -> {
          unit.captured(Module.class).iterator().next().configure(unit.get(Binder.class));
        });
  }

  @Test
  public void onStringMessage() throws Exception {
    new MockUnit(WebSocket.class, Injector.class, StringSocket.class, Binder.class, Mutant.class)
        .expect(childInjector(StringSocket.class))
        .expect(mutant(TypeLiteral.get(String.class), "string"))
        .expect(unit -> {
          StringSocket socket = unit.get(StringSocket.class);
          socket.onMessage("string");
        })
        .run(unit -> {
          new MvcWebSocket(unit.get(WebSocket.class), StringSocket.class)
              .onMessage(unit.get(Mutant.class));
        }, unit -> {
          unit.captured(Module.class).iterator().next().configure(unit.get(Binder.class));
        });
  }

  @Test
  public void onPojoMessage() throws Exception {
    Pojo pojo = new Pojo();
    new MockUnit(WebSocket.class, Injector.class, PojoSocket.class, Binder.class, Mutant.class)
        .expect(childInjector(PojoSocket.class))
        .expect(mutant(TypeLiteral.get(Pojo.class), pojo))
        .expect(unit -> {
          PojoSocket socket = unit.get(PojoSocket.class);
          socket.onMessage(pojo);
        })
        .run(unit -> {
          new MvcWebSocket(unit.get(WebSocket.class), PojoSocket.class)
              .onMessage(unit.get(Mutant.class));
        }, unit -> {
          unit.captured(Module.class).iterator().next().configure(unit.get(Binder.class));
        });
  }

  @Test
  public void onListPojoMessage() throws Exception {
    Pojo pojo = new Pojo();
    new MockUnit(WebSocket.class, Injector.class, PojoListSocket.class, Binder.class, Mutant.class)
        .expect(childInjector(PojoListSocket.class))
        .expect(mutant(TypeLiteral.get(Types.listOf(Pojo.class)), ImmutableList.of(pojo)))
        .expect(unit -> {
          PojoListSocket socket = unit.get(PojoListSocket.class);
          socket.onMessage(ImmutableList.of(pojo));
        })
        .run(unit -> {
          new MvcWebSocket(unit.get(WebSocket.class), PojoListSocket.class)
              .onMessage(unit.get(Mutant.class));
        }, unit -> {
          unit.captured(Module.class).iterator().next().configure(unit.get(Binder.class));
        });
  }

  @Test(expected = IllegalArgumentException.class)
  public void messageTypeShouldFailOnWrongCallback() throws Exception {
    MvcWebSocket.messageType(ImNotACallback.class);
  }

  @Test(expected = IllegalArgumentException.class)
  public void messageTypeShouldFailOnCallbackWithoutType() throws Exception {
    MvcWebSocket.messageType(CallbackWithoutType.class);
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  private <T> Block mutant(final TypeLiteral type, final T value) {
    return unit -> {
      Mutant mutant = unit.get(Mutant.class);
      expect(mutant.<T> to(type)).andReturn(value);
    };
  }

  @Test
  public void onOpen() throws Exception {
    new MockUnit(Request.class, WebSocket.class, Injector.class, MySocket.class, Binder.class)
        .expect(childInjector(MySocket.class))
        .expect(unit -> {
          unit.get(MySocket.class).onOpen(unit.get(Request.class), unit.get(WebSocket.class));
        })
        .run(unit -> {
          new MvcWebSocket(unit.get(WebSocket.class), MySocket.class)
              .onOpen(unit.get(Request.class), unit.get(WebSocket.class));
        }, unit -> {
          unit.captured(Module.class).iterator().next().configure(unit.get(Binder.class));
        });
  }

  @Test
  public void onError() throws Exception {
    new MockUnit(Throwable.class, WebSocket.class, Injector.class, MySocket.class, Binder.class)
        .expect(childInjector(MySocket.class))
        .expect(unit -> {
          unit.get(MySocket.class).onError(unit.get(Throwable.class));
        })
        .run(unit -> {
          new MvcWebSocket(unit.get(WebSocket.class), MySocket.class)
              .onError(unit.get(Throwable.class));
        }, unit -> {
          unit.captured(Module.class).iterator().next().configure(unit.get(Binder.class));
        });
  }

  @Test
  public void shouldIgnoreOnError() throws Exception {
    new MockUnit(Throwable.class, WebSocket.class, Injector.class, StringSocket.class, Binder.class)
        .expect(childInjector(StringSocket.class))
        .run(unit -> {
          new MvcWebSocket(unit.get(WebSocket.class), StringSocket.class)
              .onError(unit.get(Throwable.class));
        }, unit -> {
          unit.captured(Module.class).iterator().next().configure(unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void newWebSocket() throws Exception {
    new MockUnit(Request.class, WebSocket.class, Injector.class, MySocket.class, Binder.class)
        .expect(unit -> {
          WebSocket ws = unit.get(WebSocket.class);
          MySocket mvc = unit.get(MySocket.class);
          mvc.onOpen(unit.get(Request.class), unit.get(WebSocket.class));
          ws.onClose(isA(WebSocket.OnClose.class));
          ws.onError(isA(WebSocket.OnError.class));
          ws.onMessage(isA(WebSocket.OnMessage.class));
        })
        .expect(childInjector(MySocket.class))
        .run(unit -> {
          MvcWebSocket.newWebSocket(MySocket.class)
              .onOpen(unit.get(Request.class), unit.get(WebSocket.class));
        }, unit -> {
          unit.captured(Module.class).iterator().next().configure(unit.get(Binder.class));
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  private <T extends WebSocket.OnMessage> Block childInjector(final Class<T> class1) {
    return unit -> {
      Injector childInjector = unit.mock(Injector.class);
      T socket = unit.get(class1);
      expect(childInjector.getInstance(class1)).andReturn(socket);

      Injector injector = unit.get(Injector.class);
      expect(injector.createChildInjector(unit.capture(Module.class))).andReturn(childInjector);

      WebSocket ws = unit.get(WebSocket.class);
      expect(ws.require(Injector.class)).andReturn(injector);

      AnnotatedBindingBuilder<WebSocket> aabbws = unit.mock(AnnotatedBindingBuilder.class);
      aabbws.toInstance(ws);

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(WebSocket.class)).andReturn(aabbws);
    };
  }
}

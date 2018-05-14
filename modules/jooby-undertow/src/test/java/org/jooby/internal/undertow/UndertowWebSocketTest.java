package org.jooby.internal.undertow;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.WebSocket;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.xnio.ChannelListener.Setter;
import org.xnio.IoUtils;
import org.xnio.Pooled;

import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UndertowWebSocket.class, CountDownLatch.class, Thread.class, WebSockets.class,
    IoUtils.class })
public class UndertowWebSocketTest {

  private MockUnit.Block config = unit -> {
    Config config = unit.get(Config.class);
    expect(config.getDuration("undertow.ws.IdleTimeout", TimeUnit.MILLISECONDS))
        .andReturn(6000L);
    expect(config.getBytes("undertow.ws.MaxBinaryBufferSize")).andReturn(60L);
    expect(config.getBytes("undertow.ws.MaxTextBufferSize")).andReturn(80L);
  };

  @SuppressWarnings("unchecked")
  private MockUnit.Block connect = unit -> {
    Setter<WebSocketChannel> setter = unit.mock(Setter.class);
    setter.set(isA(UndertowWebSocket.class));

    WebSocketChannel ws = unit.get(WebSocketChannel.class);
    ws.setIdleTimeout(6000L);
    expect(ws.getReceiveSetter()).andReturn(setter);
    ws.resumeReceives();

    unit.get(Runnable.class).run();
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Config.class)
        .expect(config)
        .run(unit -> {
          new UndertowWebSocket(unit.get(Config.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void connect() throws Exception {
    new MockUnit(Config.class, WebSocketChannel.class, Runnable.class)
        .expect(unit -> {
          CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          ready.countDown();
        })
        .expect(unit -> {
          Setter<WebSocketChannel> setter = unit.mock(Setter.class);
          setter.set(isA(UndertowWebSocket.class));

          WebSocketChannel ws = unit.get(WebSocketChannel.class);
          ws.setIdleTimeout(6000L);
          expect(ws.getReceiveSetter()).andReturn(setter);
          ws.resumeReceives();
        })
        .expect(unit -> {
          unit.get(Runnable.class).run();
        })
        .expect(config)
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onConnect(unit.get(Runnable.class));
          ws.connect(unit.get(WebSocketChannel.class));
        });
  }

  @Test
  public void maxBinaryBufferSize() throws Exception {
    new MockUnit(Config.class)
        .expect(config)
        .run(unit -> {
          assertEquals(60L,
              new UndertowWebSocket(unit.get(Config.class)).getMaxBinaryBufferSize());
        });
  }

  @Test
  public void maxTextBufferSize() throws Exception {
    new MockUnit(Config.class)
        .expect(config)
        .run(unit -> {
          assertEquals(80L, new UndertowWebSocket(unit.get(Config.class)).getMaxTextBufferSize());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void onFullTextMessage() throws Exception {
    new MockUnit(Config.class, WebSocketChannel.class, BufferedTextMessage.class, Consumer.class)
        .expect(config)
        .expect(unit -> {
          CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          ready.await();
        })
        .expect(unit -> {
          BufferedTextMessage msg = unit.get(BufferedTextMessage.class);
          expect(msg.getData()).andReturn("x");

          Consumer<String> callback = unit.get(Consumer.class);
          callback.accept("x");
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onTextMessage(unit.get(Consumer.class));
          ws.onFullTextMessage(unit.get(WebSocketChannel.class),
              unit.get(BufferedTextMessage.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void onFullTextMessageInterrupted() throws Exception {
    new MockUnit(Config.class, WebSocketChannel.class, BufferedTextMessage.class, Consumer.class)
        .expect(config)
        .expect(unit -> {
          CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          ready.await();
          expectLastCall().andThrow(new InterruptedException("intentional err"));
        })
        .expect(unit -> {
          Thread thread = unit.mock(Thread.class);
          thread.interrupt();

          unit.mockStatic(Thread.class);
          expect(Thread.currentThread()).andReturn(thread);
        })
        .expect(unit -> {
          BufferedTextMessage msg = unit.get(BufferedTextMessage.class);
          expect(msg.getData()).andReturn("x");

          Consumer<String> callback = unit.get(Consumer.class);
          callback.accept("x");
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onTextMessage(unit.get(Consumer.class));
          ws.onFullTextMessage(unit.get(WebSocketChannel.class),
              unit.get(BufferedTextMessage.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void onFullBinaryMessage() throws Exception {
    new MockUnit(Config.class, WebSocketChannel.class, BufferedBinaryMessage.class, Consumer.class)
        .expect(config)
        .expect(unit -> {
          CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          ready.await();
        })
        .expect(unit -> {

          ByteBuffer buff = ByteBuffer.wrap(new byte[0]);
          ByteBuffer[] resource = {buff };

          unit.mockStatic(WebSockets.class);
          expect(WebSockets.mergeBuffers(resource)).andReturn(buff);

          Pooled<ByteBuffer[]> pooled = unit.mock(Pooled.class);
          expect(pooled.getResource()).andReturn(resource);
          pooled.free();

          BufferedBinaryMessage msg = unit.get(BufferedBinaryMessage.class);
          expect(msg.getData()).andReturn(pooled);

          Consumer<ByteBuffer> callback = unit.get(Consumer.class);
          callback.accept(buff);
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onBinaryMessage(unit.get(Consumer.class));
          ws.onFullBinaryMessage(unit.get(WebSocketChannel.class),
              unit.get(BufferedBinaryMessage.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = IllegalStateException.class)
  public void onFullBinaryMessageFailure() throws Exception {
    new MockUnit(Config.class, WebSocketChannel.class, BufferedBinaryMessage.class, Consumer.class)
        .expect(config)
        .expect(unit -> {
          CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          ready.await();
        })
        .expect(unit -> {

          Pooled<ByteBuffer[]> pooled = unit.mock(Pooled.class);
          expect(pooled.getResource()).andThrow(new IllegalStateException("intentional err"));
          pooled.free();

          BufferedBinaryMessage msg = unit.get(BufferedBinaryMessage.class);
          expect(msg.getData()).andReturn(pooled);

        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onBinaryMessage(unit.get(Consumer.class));
          ws.onFullBinaryMessage(unit.get(WebSocketChannel.class),
              unit.get(BufferedBinaryMessage.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void onCloseMessage() throws Exception {
    new MockUnit(Config.class, WebSocketChannel.class, CloseMessage.class, BiConsumer.class)
        .expect(config)
        .expect(unit -> {
          CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          ready.await();
        })
        .expect(unit -> {
          CloseMessage msg = unit.get(CloseMessage.class);
          expect(msg.getCode()).andReturn(1000);
          expect(msg.getReason()).andReturn(null);

          BiConsumer<Integer, Optional<String>> callback = unit.get(BiConsumer.class);
          callback.accept(1000, Optional.empty());
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onCloseMessage(unit.get(BiConsumer.class));
          ws.onCloseMessage(unit.get(CloseMessage.class),
              unit.get(WebSocketChannel.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void onError() throws Exception {
    Throwable cause = new IllegalStateException("intentional err");
    new MockUnit(Config.class, WebSocketChannel.class, CloseMessage.class, Consumer.class)
        .expect(config)
        .expect(unit -> {
          CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          ready.await();
        })
        .expect(unit -> {
          Consumer<Throwable> callback = unit.get(Consumer.class);
          callback.accept(cause);
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onErrorMessage(unit.get(Consumer.class));
          ws.onError(unit.get(WebSocketChannel.class), cause);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void close() throws Exception {
    new MockUnit(Config.class, WebSocketChannel.class, CloseMessage.class, Consumer.class,
        Runnable.class)
        .expect(config)
        .expect(unit -> {
          CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          ready.countDown();
        })
        .expect(unit -> {
          Setter<WebSocketChannel> setter = unit.mock(Setter.class);
          setter.set(isA(UndertowWebSocket.class));

          WebSocketChannel ws = unit.get(WebSocketChannel.class);
          ws.setIdleTimeout(6000L);
          expect(ws.getReceiveSetter()).andReturn(setter);
          ws.resumeReceives();
        })
        .expect(unit -> {
          unit.get(Runnable.class).run();
        })
        .expect(unit -> {
          unit.mockStatic(WebSockets.class);
          WebSockets.sendClose(eq(1000), eq("reason"), eq(unit.get(WebSocketChannel.class)),
              unit.capture(WebSocketCallback.class));
        })
        .expect(unit -> {
          unit.mockStatic(IoUtils.class);
          IoUtils.safeClose(unit.get(WebSocketChannel.class));
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onConnect(unit.get(Runnable.class));
          ws.connect(unit.get(WebSocketChannel.class));
          ws.close(1000, "reason");
        }, unit -> {
          WebSocketCallback<Void> callback = unit.captured(WebSocketCallback.class).iterator()
              .next();
          callback.complete(unit.get(WebSocketChannel.class), null);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void closeWithErr() throws Exception {
    Throwable cause = new IllegalStateException("intentional err");
    new MockUnit(Config.class, WebSocketChannel.class, CloseMessage.class, Consumer.class,
        Runnable.class)
        .expect(config)
        .expect(unit -> {
          CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          ready.countDown();
        })
        .expect(unit -> {
          Setter<WebSocketChannel> setter = unit.mock(Setter.class);
          setter.set(isA(UndertowWebSocket.class));

          WebSocketChannel ws = unit.get(WebSocketChannel.class);
          ws.setIdleTimeout(6000L);
          expect(ws.getReceiveSetter()).andReturn(setter);
          ws.resumeReceives();
        })
        .expect(unit -> {
          unit.get(Runnable.class).run();
        })
        .expect(unit -> {
          unit.mockStatic(WebSockets.class);
          WebSockets.sendClose(eq(1000), eq("reason"), eq(unit.get(WebSocketChannel.class)),
              unit.capture(WebSocketCallback.class));
        })
        .expect(unit -> {
          unit.mockStatic(IoUtils.class);

          IoUtils.safeClose(unit.get(WebSocketChannel.class));
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onConnect(unit.get(Runnable.class));
          ws.connect(unit.get(WebSocketChannel.class));
          ws.close(1000, "reason");
        }, unit -> {
          WebSocketCallback<Void> callback = unit.captured(WebSocketCallback.class).iterator()
              .next();
          callback.onError(unit.get(WebSocketChannel.class), null, cause);
        });
  }

  @Test
  public void resume() throws Exception {
    new MockUnit(Config.class, WebSocketChannel.class, Runnable.class)
        .expect(config)
        .expect(connect)
        .expect(unit -> {
          WebSocketChannel ch = unit.get(WebSocketChannel.class);
          ch.resumeReceives();
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onConnect(unit.get(Runnable.class));
          ws.connect(unit.get(WebSocketChannel.class));
          ws.resume();
        });
  }

  @Test
  public void isOpen() throws Exception {
    new MockUnit(Config.class, WebSocketChannel.class, Runnable.class)
        .expect(config)
        .expect(connect)
        .expect(unit -> {
          WebSocketChannel ch = unit.get(WebSocketChannel.class);
          expect(ch.isOpen()).andReturn(true);
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onConnect(unit.get(Runnable.class));
          ws.connect(unit.get(WebSocketChannel.class));
          ws.isOpen();
        });
  }

  @Test
  public void pause() throws Exception {
    new MockUnit(Config.class, WebSocketChannel.class, Runnable.class)
        .expect(config)
        .expect(connect)
        .expect(unit -> {
          WebSocketChannel ch = unit.get(WebSocketChannel.class);
          ch.suspendReceives();
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onConnect(unit.get(Runnable.class));
          ws.connect(unit.get(WebSocketChannel.class));
          ws.pause();
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void terminate() throws Exception {
    new MockUnit(Config.class, WebSocketChannel.class, Runnable.class, BiConsumer.class)
        .expect(config)
        .expect(connect)
        .expect(unit -> {
          BiConsumer<Integer, Optional<String>> callback = unit.get(BiConsumer.class);
          callback.accept(1006, Optional.of("Harsh disconnect"));
        })
        .expect(unit -> {
          unit.mockStatic(IoUtils.class);
          IoUtils.safeClose(unit.get(WebSocketChannel.class));
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onConnect(unit.get(Runnable.class));
          ws.connect(unit.get(WebSocketChannel.class));
          ws.onCloseMessage(unit.get(BiConsumer.class));
          ws.terminate();
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void sendText() throws Exception {
    new MockUnit(Config.class, WebSocketChannel.class, CloseMessage.class, Consumer.class,
        Runnable.class, WebSocket.SuccessCallback.class, WebSocket.OnError.class)
        .expect(config)
        .expect(connect)
        .expect(unit -> {
          unit.mockStatic(WebSockets.class);
          WebSockets.sendText(eq("data"), eq(unit.get(WebSocketChannel.class)),
              unit.capture(WebSocketCallback.class));
        })
        .expect(unit -> {
          unit.get(WebSocket.SuccessCallback.class).invoke();
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onConnect(unit.get(Runnable.class));
          ws.connect(unit.get(WebSocketChannel.class));
          ws.sendText("data", unit.get(WebSocket.SuccessCallback.class),
              unit.get(WebSocket.OnError.class));
        }, unit -> {
          WebSocketCallback<Void> callback = unit.captured(WebSocketCallback.class).iterator()
              .next();
          callback.complete(unit.get(WebSocketChannel.class), null);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void sendTextCallbackErr() throws Exception {
    new MockUnit(Config.class, WebSocketChannel.class, CloseMessage.class, Consumer.class,
        Runnable.class, WebSocket.SuccessCallback.class, WebSocket.OnError.class)
        .expect(config)
        .expect(connect)
        .expect(unit -> {
          unit.mockStatic(WebSockets.class);
          WebSockets.sendText(eq("data"), eq(unit.get(WebSocketChannel.class)),
              unit.capture(WebSocketCallback.class));
        })
        .expect(unit -> {
          unit.get(WebSocket.SuccessCallback.class).invoke();
          expectLastCall().andThrow(new IllegalStateException("intentional err"));
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onConnect(unit.get(Runnable.class));
          ws.connect(unit.get(WebSocketChannel.class));
          ws.sendText("data", unit.get(WebSocket.SuccessCallback.class),
              unit.get(WebSocket.OnError.class));
        }, unit -> {
          WebSocketCallback<Void> callback = unit.captured(WebSocketCallback.class).iterator()
              .next();
          callback.complete(unit.get(WebSocketChannel.class), null);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void sendBytes() throws Exception {
    ByteBuffer data = ByteBuffer.wrap(new byte[0]);
    new MockUnit(Config.class, WebSocketChannel.class, CloseMessage.class, Consumer.class,
        Runnable.class, WebSocket.SuccessCallback.class, WebSocket.OnError.class)
        .expect(config)
        .expect(connect)
        .expect(unit -> {
          unit.mockStatic(WebSockets.class);
          WebSockets.sendBinary(eq(data), eq(unit.get(WebSocketChannel.class)),
              unit.capture(WebSocketCallback.class));
        })
        .expect(unit -> {
          unit.get(WebSocket.SuccessCallback.class).invoke();
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onConnect(unit.get(Runnable.class));
          ws.connect(unit.get(WebSocketChannel.class));
          ws.sendBytes(data, unit.get(WebSocket.SuccessCallback.class),
              unit.get(WebSocket.OnError.class));
        }, unit -> {
          WebSocketCallback<Void> callback = unit.captured(WebSocketCallback.class).iterator()
              .next();
          callback.complete(unit.get(WebSocketChannel.class), null);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void sendTextErrCallback() throws Exception {
    Throwable cause = new IllegalStateException("intentional err");
    new MockUnit(Config.class, WebSocketChannel.class, CloseMessage.class, Consumer.class,
        Runnable.class, WebSocket.SuccessCallback.class, WebSocket.OnError.class)
        .expect(config)
        .expect(connect)
        .expect(unit -> {
          unit.mockStatic(WebSockets.class);
          WebSockets.sendText(eq("data"), eq(unit.get(WebSocketChannel.class)),
              unit.capture(WebSocketCallback.class));
        })
        .expect(unit -> {
          unit.get(WebSocket.OnError.class).onError(cause);
        })
        .run(unit -> {
          UndertowWebSocket ws = new UndertowWebSocket(unit.get(Config.class));
          ws.onConnect(unit.get(Runnable.class));
          ws.connect(unit.get(WebSocketChannel.class));
          ws.sendText("data", unit.get(WebSocket.SuccessCallback.class),
              unit.get(WebSocket.OnError.class));
        }, unit -> {
          WebSocketCallback<Void> callback = unit.captured(WebSocketCallback.class).iterator()
              .next();
          callback.onError(unit.get(WebSocketChannel.class), null, cause);
        });
  }
}

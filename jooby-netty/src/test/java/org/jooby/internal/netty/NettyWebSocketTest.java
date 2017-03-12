package org.jooby.internal.netty;

import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.WebSocket;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.util.Attribute;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NettyWebSocket.class, CloseWebSocketFrame.class, Unpooled.class,
    CountDownLatch.class, Thread.class })
public class NettyWebSocketTest {

  private Block channel = unit -> {
    Channel channel = unit.mock(Channel.class);
    unit.registerMock(Channel.class, channel);

    ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
    expect(ctx.channel()).andReturn(channel).times(1, 2);
  };

  private MockUnit.Block close = unit -> {
    Channel ch = unit.get(Channel.class);

    CloseWebSocketFrame frame = unit.mockConstructor(
        CloseWebSocketFrame.class,
        new Class[]{int.class, String.class },
        1001, "normal");

    ChannelFuture future = unit.mock(ChannelFuture.class);
    expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future);

    WebSocketServerHandshaker handshaker = unit.get(WebSocketServerHandshaker.class);
    expect(handshaker.close(ch, frame)).andReturn(future);
  };

  @SuppressWarnings("unchecked")
  @Test
  public void close() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class)
        .expect(channel)
        .expect(close)
        .expect(unit -> {
          Attribute<NettyWebSocket> attr = unit.mock(Attribute.class);
          attr.set(null);

          Channel ctx = unit.get(Channel.class);
          expect(ctx.attr(NettyWebSocket.KEY)).andReturn(attr);
        })
        .run(unit -> {
          new NettyWebSocket(
              unit.get(ChannelHandlerContext.class),
              unit.get(WebSocketServerHandshaker.class),
              unit.get(Consumer.class)).close(1001, "normal");
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void closeNoAttr() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class)
        .expect(channel)
        .expect(close)
        .expect(unit -> {
          Channel ctx = unit.get(Channel.class);
          expect(ctx.attr(NettyWebSocket.KEY)).andReturn(null);
        })
        .run(unit -> {
          new NettyWebSocket(
              unit.get(ChannelHandlerContext.class),
              unit.get(WebSocketServerHandshaker.class),
              unit.get(Consumer.class)).close(1001, "normal");
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void resume() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class)
        .expect(unit -> {
          ChannelConfig chconf = unit.mock(ChannelConfig.class);
          expect(chconf.isAutoRead()).andReturn(false);
          expect(chconf.setAutoRead(true)).andReturn(chconf);

          Channel ch = unit.mock(Channel.class);
          expect(ch.config()).andReturn(chconf);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.channel()).andReturn(ch);
        })
        .run(unit -> {
          new NettyWebSocket(
              unit.get(ChannelHandlerContext.class),
              unit.get(WebSocketServerHandshaker.class),
              unit.get(Consumer.class)).resume();
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void resumeIgnored() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class)
        .expect(unit -> {
          ChannelConfig chconf = unit.mock(ChannelConfig.class);
          expect(chconf.isAutoRead()).andReturn(true);

          Channel ch = unit.mock(Channel.class);
          expect(ch.config()).andReturn(chconf);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.channel()).andReturn(ch);
        })
        .run(unit -> {
          new NettyWebSocket(
              unit.get(ChannelHandlerContext.class),
              unit.get(WebSocketServerHandshaker.class),
              unit.get(Consumer.class)).resume();
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void pause() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class)
        .expect(unit -> {
          ChannelConfig chconf = unit.mock(ChannelConfig.class);
          expect(chconf.isAutoRead()).andReturn(true);
          expect(chconf.setAutoRead(false)).andReturn(chconf);

          Channel ch = unit.mock(Channel.class);
          expect(ch.config()).andReturn(chconf);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.channel()).andReturn(ch);
        })
        .run(unit -> {
          new NettyWebSocket(
              unit.get(ChannelHandlerContext.class),
              unit.get(WebSocketServerHandshaker.class),
              unit.get(Consumer.class)).pause();
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void pauseIgnored() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class)
        .expect(unit -> {
          ChannelConfig chconf = unit.mock(ChannelConfig.class);
          expect(chconf.isAutoRead()).andReturn(false);

          Channel ch = unit.mock(Channel.class);
          expect(ch.config()).andReturn(chconf);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.channel()).andReturn(ch);
        })
        .run(unit -> {
          new NettyWebSocket(
              unit.get(ChannelHandlerContext.class),
              unit.get(WebSocketServerHandshaker.class),
              unit.get(Consumer.class)).pause();
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void terminate() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class,
        BiConsumer.class)
            .expect(unit -> {
              BiConsumer<Integer, Optional<String>> callback = unit.get(BiConsumer.class);
              callback.accept(1006, Optional.of("Harsh disconnect"));

              ChannelFuture future = unit.mock(ChannelFuture.class);
              expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future);

              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.disconnect()).andReturn(future);
            })
            .run(unit -> {
              NettyWebSocket ws = new NettyWebSocket(
                  unit.get(ChannelHandlerContext.class),
                  unit.get(WebSocketServerHandshaker.class),
                  unit.get(Consumer.class));
              ws.onCloseMessage(unit.get(BiConsumer.class));
              ws.terminate();
            });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void sendBytes() throws Exception {
    ByteBuffer buffer = ByteBuffer.wrap(new byte[]{'a', 'b', 'c' });
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class,
        WebSocket.SuccessCallback.class, WebSocket.OnError.class, Future.class)
            .expect(unit -> {
              ByteBuf byteBuf = unit.mock(ByteBuf.class);

              unit.mockStatic(Unpooled.class);
              expect(Unpooled.wrappedBuffer(buffer)).andReturn(byteBuf);

              ChannelFuture future = unit.mock(ChannelFuture.class);
              expect(future.addListener(unit.capture(GenericFutureListener.class)))
                  .andReturn(future);

              BinaryWebSocketFrame frame = unit.mockConstructor(BinaryWebSocketFrame.class,
                  new Class[]{ByteBuf.class }, byteBuf);
              Channel ch = unit.mock(Channel.class);
              expect(ch.writeAndFlush(frame)).andReturn(future);

              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.channel()).andReturn(ch);
            })
            .expect(unit -> {
              Future future = unit.get(Future.class);
              expect(future.isSuccess()).andReturn(true);
              WebSocket.SuccessCallback success = unit.get(WebSocket.SuccessCallback.class);
              success.invoke();
            })
            .run(
                unit -> {
                  NettyWebSocket ws = new NettyWebSocket(
                      unit.get(ChannelHandlerContext.class),
                      unit.get(WebSocketServerHandshaker.class),
                      unit.get(Consumer.class));
                  ws.sendBytes(buffer, unit.get(WebSocket.SuccessCallback.class),
                      unit.get(WebSocket.OnError.class));
                },
                unit -> {
                  GenericFutureListener listener = unit.captured(GenericFutureListener.class)
                      .iterator().next();
                  listener.operationComplete(unit.get(Future.class));
                });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void sendString() throws Exception {
    String data = "abc";
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class,
        WebSocket.SuccessCallback.class, WebSocket.OnError.class, Future.class)
            .expect(unit -> {

              ChannelFuture future = unit.mock(ChannelFuture.class);
              expect(future.addListener(unit.capture(GenericFutureListener.class)))
                  .andReturn(future);

              TextWebSocketFrame frame = unit.mockConstructor(TextWebSocketFrame.class,
                  new Class[]{String.class }, data);
              Channel ch = unit.mock(Channel.class);
              expect(ch.writeAndFlush(frame)).andReturn(future);

              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.channel()).andReturn(ch);
            })
            .expect(unit -> {
              Future future = unit.get(Future.class);
              expect(future.isSuccess()).andReturn(true);
              WebSocket.SuccessCallback success = unit.get(WebSocket.SuccessCallback.class);
              success.invoke();
            })
            .run(
                unit -> {
                  NettyWebSocket ws = new NettyWebSocket(
                      unit.get(ChannelHandlerContext.class),
                      unit.get(WebSocketServerHandshaker.class),
                      unit.get(Consumer.class));
                  ws.sendText(data, unit.get(WebSocket.SuccessCallback.class),
                      unit.get(WebSocket.OnError.class));
                },
                unit -> {
                  GenericFutureListener listener = unit.captured(GenericFutureListener.class)
                      .iterator().next();
                  listener.operationComplete(unit.get(Future.class));
                });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void sendBytesFailure() throws Exception {
    ByteBuffer buffer = ByteBuffer.wrap(new byte[]{'a', 'b', 'c' });
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class,
        WebSocket.SuccessCallback.class, WebSocket.OnError.class, Future.class)
            .expect(unit -> {
              ByteBuf byteBuf = unit.mock(ByteBuf.class);

              unit.mockStatic(Unpooled.class);
              expect(Unpooled.wrappedBuffer(buffer)).andReturn(byteBuf);

              ChannelFuture future = unit.mock(ChannelFuture.class);
              expect(future.addListener(unit.capture(GenericFutureListener.class)))
                  .andReturn(future);

              BinaryWebSocketFrame frame = unit.mockConstructor(BinaryWebSocketFrame.class,
                  new Class[]{ByteBuf.class }, byteBuf);
              Channel ch = unit.mock(Channel.class);
              expect(ch.writeAndFlush(frame)).andReturn(future);

              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.channel()).andReturn(ch);
            })
            .expect(unit -> {
              Throwable cause = new NullPointerException();
              Future future = unit.get(Future.class);
              expect(future.isSuccess()).andReturn(false);
              expect(future.cause()).andReturn(cause);
              WebSocket.OnError err = unit.get(WebSocket.OnError.class);
              err.onError(cause);
            })
            .run(
                unit -> {
                  NettyWebSocket ws = new NettyWebSocket(
                      unit.get(ChannelHandlerContext.class),
                      unit.get(WebSocketServerHandshaker.class),
                      unit.get(Consumer.class));
                  ws.sendBytes(buffer, unit.get(WebSocket.SuccessCallback.class),
                      unit.get(WebSocket.OnError.class));
                },
                unit -> {
                  GenericFutureListener listener = unit.captured(GenericFutureListener.class)
                      .iterator().next();
                  listener.operationComplete(unit.get(Future.class));
                });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void sendStringFailure() throws Exception {
    String data = "abc";
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class,
        WebSocket.SuccessCallback.class, WebSocket.OnError.class, Future.class)
            .expect(unit -> {

              ChannelFuture future = unit.mock(ChannelFuture.class);
              expect(future.addListener(unit.capture(GenericFutureListener.class)))
                  .andReturn(future);

              TextWebSocketFrame frame = unit.mockConstructor(TextWebSocketFrame.class,
                  new Class[]{String.class }, data);
              Channel ch = unit.mock(Channel.class);
              expect(ch.writeAndFlush(frame)).andReturn(future);

              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.channel()).andReturn(ch);
            })
            .expect(unit -> {
              Throwable cause = new NullPointerException();
              Future future = unit.get(Future.class);
              expect(future.isSuccess()).andReturn(false);
              expect(future.cause()).andReturn(cause);
              WebSocket.OnError err = unit.get(WebSocket.OnError.class);
              err.onError(cause);
            })
            .run(
                unit -> {
                  NettyWebSocket ws = new NettyWebSocket(
                      unit.get(ChannelHandlerContext.class),
                      unit.get(WebSocketServerHandshaker.class),
                      unit.get(Consumer.class));
                  ws.sendText(data, unit.get(WebSocket.SuccessCallback.class),
                      unit.get(WebSocket.OnError.class));
                },
                unit -> {
                  GenericFutureListener listener = unit.captured(GenericFutureListener.class)
                      .iterator().next();
                  listener.operationComplete(unit.get(Future.class));
                });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void isOpen() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class)
        .expect(unit -> {
          Channel ch = unit.mock(Channel.class);
          expect(ch.isOpen()).andReturn(true);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.channel()).andReturn(ch);
        })
        .run(unit -> {
          assertEquals(true, new NettyWebSocket(
              unit.get(ChannelHandlerContext.class),
              unit.get(WebSocketServerHandshaker.class),
              unit.get(Consumer.class)).isOpen());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void isNoOpen() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class)
        .expect(unit -> {
          Channel ch = unit.mock(Channel.class);
          expect(ch.isOpen()).andReturn(false);

          ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
          expect(ctx.channel()).andReturn(ch);
        })
        .run(unit -> {
          assertEquals(false, new NettyWebSocket(
              unit.get(ChannelHandlerContext.class),
              unit.get(WebSocketServerHandshaker.class),
              unit.get(Consumer.class)).isOpen());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void connect() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class,
        CountDownLatch.class, Runnable.class)
            .expect(unit -> {
              CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
                  new Class[]{int.class }, 1);
              ready.countDown();

              unit.get(Runnable.class).run();
            })
            .run(unit -> {
              NettyWebSocket ws = new NettyWebSocket(
                  unit.get(ChannelHandlerContext.class),
                  unit.get(WebSocketServerHandshaker.class),
                  unit.get(Consumer.class));
              ws.onConnect(unit.get(Runnable.class));
              ws.connect();
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void hankshake() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class,
        CountDownLatch.class, Consumer.class)
            .expect(unit -> {

              unit.get(Consumer.class).accept(isA(NettyWebSocket.class));
            })
            .run(unit -> {
              NettyWebSocket ws = new NettyWebSocket(
                  unit.get(ChannelHandlerContext.class),
                  unit.get(WebSocketServerHandshaker.class),
                  unit.get(Consumer.class));
              ws.hankshake();
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void handleTextFrame() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class,
        TextWebSocketFrame.class)
            .expect(unit -> {
              CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
                  new Class[]{int.class }, 1);
              ready.await();

              TextWebSocketFrame frame = unit.get(TextWebSocketFrame.class);
              expect(frame.text()).andReturn("text");

              Consumer<String> callback = unit.get(Consumer.class);
              callback.accept("text");
            })
            .run(unit -> {
              NettyWebSocket ws = new NettyWebSocket(
                  unit.get(ChannelHandlerContext.class),
                  unit.get(WebSocketServerHandshaker.class),
                  unit.get(Consumer.class));
              ws.onTextMessage(unit.get(Consumer.class));
              ws.handle(unit.get(TextWebSocketFrame.class));
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void handleBinaryFrame() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class,
        BinaryWebSocketFrame.class)
            .expect(unit -> {
              CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
                  new Class[]{int.class }, 1);
              ready.await();

              ByteBuffer nioBuff = ByteBuffer.wrap(new byte[0]);

              ByteBuf buff = unit.mock(ByteBuf.class);
              expect(buff.nioBuffer()).andReturn(nioBuff);

              BinaryWebSocketFrame frame = unit.get(BinaryWebSocketFrame.class);
              expect(frame.content()).andReturn(buff);

              Consumer<ByteBuffer> callback = unit.get(Consumer.class);
              callback.accept(nioBuff);
            })
            .run(unit -> {
              NettyWebSocket ws = new NettyWebSocket(
                  unit.get(ChannelHandlerContext.class),
                  unit.get(WebSocketServerHandshaker.class),
                  unit.get(Consumer.class));
              ws.onBinaryMessage(unit.get(Consumer.class));
              ws.handle(unit.get(BinaryWebSocketFrame.class));
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void handleInterruped() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class,
        WebSocketFrame.class)
            .expect(unit -> {
              CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
                  new Class[]{int.class }, 1);
              ready.await();
              expectLastCall().andThrow(new InterruptedException("intentional err"));

              Thread thread = unit.mock(Thread.class);
              thread.interrupt();

              unit.mockStatic(Thread.class);
              expect(Thread.currentThread()).andReturn(thread);
            })
            .run(unit -> {
              NettyWebSocket ws = new NettyWebSocket(
                  unit.get(ChannelHandlerContext.class),
                  unit.get(WebSocketServerHandshaker.class),
                  unit.get(Consumer.class));
              ws.handle(unit.get(WebSocketFrame.class));
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void handleCloseFrame() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class,
        CloseWebSocketFrame.class, BiConsumer.class)
            .expect(unit -> {
              CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
                  new Class[]{int.class }, 1);
              ready.await();

              CloseWebSocketFrame retain = unit.get(CloseWebSocketFrame.class);
              expect(retain.statusCode()).andReturn(-1);
              expect(retain.reasonText()).andReturn(null);

              CloseWebSocketFrame frame = unit.get(CloseWebSocketFrame.class);
              expect(frame.retain()).andReturn(retain);

              BiConsumer<Integer, Optional<String>> callback = unit.get(BiConsumer.class);
              callback.accept(1000, Optional.empty());

              Channel ch = unit.mock(Channel.class);

              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.channel()).andReturn(ch);

              ChannelFuture future = unit.mock(ChannelFuture.class);
              expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future);

              WebSocketServerHandshaker handshaker = unit.get(WebSocketServerHandshaker.class);
              expect(handshaker.close(ch, retain)).andReturn(future);
            })
            .run(unit -> {
              NettyWebSocket ws = new NettyWebSocket(
                  unit.get(ChannelHandlerContext.class),
                  unit.get(WebSocketServerHandshaker.class),
                  unit.get(Consumer.class));
              ws.onCloseMessage(unit.get(BiConsumer.class));
              ws.handle(unit.get(CloseWebSocketFrame.class));
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void handleCloseWithStatusFrame() throws Exception {
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class,
        CloseWebSocketFrame.class, BiConsumer.class)
            .expect(unit -> {
              CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
                  new Class[]{int.class }, 1);
              ready.await();

              CloseWebSocketFrame retain = unit.get(CloseWebSocketFrame.class);
              expect(retain.statusCode()).andReturn(1001);
              expect(retain.reasonText()).andReturn("normal");

              CloseWebSocketFrame frame = unit.get(CloseWebSocketFrame.class);
              expect(frame.retain()).andReturn(retain);

              BiConsumer<Integer, Optional<String>> callback = unit.get(BiConsumer.class);
              callback.accept(1001, Optional.of("normal"));

              Channel ch = unit.mock(Channel.class);

              ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
              expect(ctx.channel()).andReturn(ch);

              ChannelFuture future = unit.mock(ChannelFuture.class);
              expect(future.addListener(FIRE_EXCEPTION_ON_FAILURE)).andReturn(future);

              WebSocketServerHandshaker handshaker = unit.get(WebSocketServerHandshaker.class);
              expect(handshaker.close(ch, retain)).andReturn(future);
            })
            .run(unit -> {
              NettyWebSocket ws = new NettyWebSocket(
                  unit.get(ChannelHandlerContext.class),
                  unit.get(WebSocketServerHandshaker.class),
                  unit.get(Consumer.class));
              ws.onCloseMessage(unit.get(BiConsumer.class));
              ws.handle(unit.get(CloseWebSocketFrame.class));
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void handleException() throws Exception {
    Throwable cause = new NullPointerException("intentional err");
    new MockUnit(ChannelHandlerContext.class, WebSocketServerHandshaker.class, Consumer.class)
        .expect(unit -> {
          CountDownLatch ready = unit.mockConstructor(CountDownLatch.class,
              new Class[]{int.class }, 1);
          ready.await();

          Consumer<Throwable> callback = unit.get(Consumer.class);
          callback.accept(cause);
        })
        .run(unit -> {
          NettyWebSocket ws = new NettyWebSocket(
              unit.get(ChannelHandlerContext.class),
              unit.get(WebSocketServerHandshaker.class),
              unit.get(Consumer.class));
          ws.onErrorMessage(unit.get(Consumer.class));
          ws.handle(cause);
        });
  }

}

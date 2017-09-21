package org.jooby.internal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.EventExecutor;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

@RunWith(PowerMockRunner.class)
@PrepareForTest({NettySse.class, DefaultHttpHeaders.class, DefaultHttpResponse.class})
public class NettySseTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(ChannelHandlerContext.class)
      .run(unit -> {
        new NettySse(unit.get(ChannelHandlerContext.class));
      });
  }

  @Test
  public void close() throws Exception {
    new MockUnit(ChannelHandlerContext.class)
      .expect(unit -> {
        expect(unit.get(ChannelHandlerContext.class).close()).andReturn(null);
      })
      .run(unit -> {
        new NettySse(unit.get(ChannelHandlerContext.class))
          .close();
      });
  }

  @Test
  public void handshake() throws Exception {
    new MockUnit(ChannelHandlerContext.class, EventExecutor.class, Runnable.class)
      .expect(unit -> {
        DefaultHttpHeaders headers = unit.constructor(DefaultHttpHeaders.class)
          .build();

        expect(headers.set(HttpHeaderNames.CONNECTION, "Close")).andReturn(headers);
        expect(headers.set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream; charset=utf-8"))
          .andReturn(headers);

        DefaultHttpResponse rsp = unit.constructor(DefaultHttpResponse.class)
          .args(HttpVersion.class, HttpResponseStatus.class, HttpHeaders.class)
          .build(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, headers);

        EventExecutor executor = unit.get(EventExecutor.class);
        executor.execute(unit.get(Runnable.class));

        ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
        expect(ctx.writeAndFlush(rsp)).andReturn(null);
        expect(ctx.executor()).andReturn(executor);
      })
      .run(unit -> {
        new NettySse(unit.get(ChannelHandlerContext.class))
          .handshake(unit.get(Runnable.class));
      });
  }

  @Test
  public void send() throws Exception {
    byte[] bytes = {0};
    CountDownLatch latch = new CountDownLatch(1);
    new MockUnit(ChannelHandlerContext.class, ChannelFuture.class)
      .expect(unit -> {
        ChannelFuture future = unit.get(ChannelFuture.class);
        expect(future.isSuccess()).andReturn(true);
        expect(future.addListener(unit.capture(ChannelFutureListener.class))).andReturn(future);

        ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
        expect(ctx.writeAndFlush(isA(ByteBuf.class))).andReturn(future);
      })
      .run(unit -> {
        new NettySse(unit.get(ChannelHandlerContext.class))
          .send(Optional.of("id"), bytes).whenComplete((id, x) -> {
          if (x == null) {
            assertEquals("id", id.get());
            latch.countDown();
          }
        });
      }, unit -> {
        ChannelFutureListener listener = unit.captured(ChannelFutureListener.class).iterator()
          .next();
        listener.operationComplete(unit.get(ChannelFuture.class));
        latch.await();
      });
  }

  @Test(expected = IllegalStateException.class)
  public void sendErr() throws Exception {
    byte[] bytes = {0};
    new MockUnit(ChannelHandlerContext.class)
      .expect(unit -> {
        ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
        expect(ctx.writeAndFlush(isA(ByteBuf.class)))
          .andThrow(new IllegalStateException("intentional error"));
      })
      .run(unit -> {
        new NettySse(unit.get(ChannelHandlerContext.class))
          .send(Optional.of("id"), bytes);
      });
  }

  @Test
  public void sendFailure() throws Exception {
    byte[] bytes = {0};
    CountDownLatch latch = new CountDownLatch(1);
    IOException ex = new IOException("intentional err");
    new MockUnit(ChannelHandlerContext.class, ChannelFuture.class)
      .expect(unit -> {
        ChannelFuture future = unit.get(ChannelFuture.class);
        expect(future.isSuccess()).andReturn(false);
        expect(future.cause()).andReturn(ex);
        expect(future.addListener(unit.capture(ChannelFutureListener.class))).andReturn(future);

        ChannelHandlerContext ctx = unit.get(ChannelHandlerContext.class);
        expect(ctx.writeAndFlush(isA(ByteBuf.class))).andReturn(future);
      })
      .run(unit -> {
        new NettySse(unit.get(ChannelHandlerContext.class))
          .send(Optional.of("id"), bytes).whenComplete((id, cause) -> {
          if (cause != null) {
            assertEquals(ex, cause);
            latch.countDown();
          }
        });
      }, unit -> {
        ChannelFutureListener listener = unit.captured(ChannelFutureListener.class).iterator()
          .next();
        listener.operationComplete(unit.get(ChannelFuture.class));
        latch.await();
      });
  }

}

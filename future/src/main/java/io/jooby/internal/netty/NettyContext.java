package io.jooby.internal.netty;

import io.jooby.Context;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.EmptyByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

import static io.netty.buffer.Unpooled.EMPTY_BUFFER;
import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyContext implements Context {
  private final HttpHeaders setHeaders = new DefaultHttpHeaders(false);
  private final ChannelHandlerContext ctx;
  private final HttpRequest req;
  private final String path;
  private final DefaultEventExecutorGroup executor;
  private HttpResponseStatus status = HttpResponseStatus.OK;
  private boolean keepAlive;
  private boolean responseStarted;

  public NettyContext(ChannelHandlerContext ctx, DefaultEventExecutorGroup executor,
      HttpRequest req, boolean keepAlive, String path) {
    this.path = path;
    this.ctx = ctx;
    this.req = req;
    this.executor = executor;
    this.keepAlive = keepAlive;
  }

  @Nonnull @Override public final String path() {
    return path;
  }

  @Nonnull @Override public String method() {
    return req.method().name();
  }

  @Override public final boolean isInIoThread() {
    return ctx.executor().inEventLoop();
  }

  @Nonnull @Override public Executor worker() {
    return executor;
  }

  @Override public Context dispatch(Executor executor, Runnable action) {
    executor.execute(action);
    return this;
  }

  @Nonnull @Override public Context statusCode(int statusCode) {
    this.status = HttpResponseStatus.valueOf(statusCode);
    return this;
  }

  @Override public final Context type(String contentType) {
    setHeaders.set(CONTENT_TYPE, contentType);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull String data) {
    return sendByteBuf(copiedBuffer(data, StandardCharsets.UTF_8));
  }

  @Override public final Context send(String data, Charset charset) {
    return sendByteBuf(copiedBuffer(data, charset));
  }

  @Override public final Context send(byte[] data) {
    return sendByteBuf(wrappedBuffer(data));
  }

  @Override public final Context send(ByteBuffer data) {
    return sendByteBuf(wrappedBuffer(data));
  }

  @Override public boolean isResponseStarted() {
    return responseStarted;
  }

  @Nonnull @Override public Context sendStatusCode(int statusCode) {
    setHeaders.set(CONTENT_LENGTH, 0);
    return sendComplete(
        new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.valueOf(statusCode),
            EMPTY_BUFFER));
  }

  private Context sendByteBuf(ByteBuf buff) {
    setHeaders.set(CONTENT_LENGTH, buff.readableBytes());
    return sendComplete(new DefaultFullHttpResponse(HTTP_1_1, status, buff));
  }

  private Context sendComplete(HttpResponse rsp) {
    responseStarted = true;
    rsp.headers().set(setHeaders);
    if (keepAlive) {
      rsp.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
      ctx.write(rsp, ctx.voidPromise());
    } else {
      ctx.write(rsp).addListener(CLOSE);
    }
    ctx.executor().execute(ctx::flush);
    return this;
  }
}

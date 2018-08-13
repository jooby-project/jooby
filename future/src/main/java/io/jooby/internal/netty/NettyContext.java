package io.jooby.internal.netty;

import io.jooby.Context;
import io.jooby.Form;
import io.jooby.QueryString;
import io.jooby.Route;
import io.jooby.Value;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostStandardRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class NettyContext implements Context {

  private final HttpHeaders setHeaders = new DefaultHttpHeaders(false);
  private ChannelHandlerContext ctx;
  private final HttpRequest req;
  private final String path;
  private final DefaultEventExecutorGroup executor;
  private final Route route;
  private HttpResponseStatus status = HttpResponseStatus.OK;
  private boolean keepAlive;
  private boolean responseStarted;
  private final Map<String, Object> locals = new HashMap<>();
  private QueryString query;
  private Form form;

  public NettyContext(ChannelHandlerContext ctx, DefaultEventExecutorGroup executor,
      HttpRequest req, boolean keepAlive, String path, Route route) {
    this.path = path;
    this.ctx = ctx;
    this.req = req;
    this.executor = executor;
    this.keepAlive = keepAlive;
    this.route = route;
  }

  /* **********************************************************************************************
   * Request methods:
   * **********************************************************************************************
   */

  @Nonnull @Override public final String path() {
    return path;
  }

  @Nonnull @Override public Route route() {
    return route;
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

  @Nonnull @Override public QueryString query() {
    if (query == null) {
      query = Value.queryString(req.uri());
    }
    return query;
  }

  @Nonnull @Override public Form form() {
    if (form == null) {
      form = new Form();
      decodeForm(new HttpPostStandardRequestDecoder(req), form);
    }
    return form;
  }

  private void decodeForm(InterfaceHttpPostRequestDecoder decoder, Value.Object form) {
    try {
      while (decoder.hasNext()) {
        HttpData next = (HttpData) decoder.next();
        form.put(next.getName(), next.getString(StandardCharsets.UTF_8));
      }
    } catch (HttpPostRequestDecoder.EndOfDataDecoderException x) {
      // ignore, silly netty
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    } finally {
      decoder.destroy();
    }
  }

  @Nonnull @Override public Map<String, Object> locals() {
    return locals;
  }

  @Nonnull @Override public Route.Filter gzip() {
    return next -> ctx -> {
      if (req.headers().contains(HttpHeaderNames.ACCEPT_ENCODING)) {
        this.ctx.pipeline().addBefore("handler", "gzip", gzip(this.ctx, req));
      }
      return next.apply(ctx);
    };
  }

  /* **********************************************************************************************
   * Response methods:
   * **********************************************************************************************
   */

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
    DefaultFullHttpResponse rsp = new DefaultFullHttpResponse(HTTP_1_1,
        HttpResponseStatus.valueOf(statusCode));
    rsp.headers().set(CONTENT_LENGTH, 0);
    ctx.writeAndFlush(rsp).addListener(CLOSE);
    return this;
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

  private static HttpContentCompressor gzip(ChannelHandlerContext ctx, HttpRequest req)
      throws Exception {
    HttpContentCompressor compressor = new HttpContentCompressor() {
      @Override protected void encode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out)
          throws Exception {
        super.encode(ctx, msg, out);
        // TODO: is there a better way?
        if (msg instanceof LastHttpContent) {
          ctx.pipeline().remove(this);
        }
      }
    };
    // TODO: is there a better way?
    // Initialize
    compressor.channelRead(ctx, req);
    return compressor;
  }
}

package io.jooby.internal.netty;

import io.jooby.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.ReferenceCounted;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executor;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jooby.funzy.Throwing.throwingConsumer;

public class NettyContext extends BaseContext {
  private final HttpHeaders setHeaders = new DefaultHttpHeaders(false);
  private final Route.RootErrorHandler errorHandler;
  private final ChannelHandlerContext ctx;
  private HttpRequest req;
  private final String path;
  private final DefaultEventExecutorGroup executor;
  private HttpResponseStatus status = HttpResponseStatus.OK;
  private boolean keepAlive;
  private boolean responseStarted;
  private QueryString query;
  private Form form;
  private Multipart multipart;
  private List<Value.Upload> files;
  private Value.Object headers;

  public NettyContext(ChannelHandlerContext ctx, DefaultEventExecutorGroup executor,
      HttpRequest req, Route.RootErrorHandler errorHandler, String path) {
    this.path = path;
    this.ctx = ctx;
    this.req = req;
    this.executor = executor;
    this.keepAlive = isKeepAlive(req);
    this.errorHandler = errorHandler;
  }

  /* **********************************************************************************************
   * Request methods:
   * **********************************************************************************************
   */

  @Nonnull @Override public String method() {
    return req.method().asciiName().toUpperCase().toString();
  }

  @Nonnull @Override public final String path() {
    return path;
  }

  @Override public final boolean isInIoThread() {
    return ctx.channel().eventLoop().inEventLoop();
  }

  @Nonnull @Override public Executor worker() {
    return executor;
  }

  @Override public Context dispatch(Executor executor, Runnable action) {
    executor.execute(action);
    return this;
  }

  @Nonnull @Override public Context detach(@Nonnull Runnable action) {
    action.run();
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
      decodeForm(req, new HttpPostStandardRequestDecoder(req), form);
    }
    return form;
  }

  @Nonnull @Override public Multipart multipart() {
    requireBlocking();
    if (multipart == null) {
      multipart = new Multipart();
      form = multipart;
      HttpDataFactory factory = new DefaultHttpDataFactory(_16KB);
      decodeForm(req, new HttpPostMultipartRequestDecoder(factory, req, StandardCharsets.UTF_8),
          multipart);
    }
    return multipart;
  }

  @Nonnull @Override public Value header(@Nonnull String name) {
    return Value.create(name, req.headers().getAll(name));
  }

  @Nonnull @Override public Value headers() {
    if (headers == null) {
      headers = Value.headers();
      HttpHeaders headers = req.headers();
      Set<String> names = headers.names();
      for (String name : names) {
        this.headers.put(name, headers.getAll(name));
      }
    }
    return headers;
  }

  @Nonnull @Override public Body body() {
    requireBlocking();
    return Body.of(new ByteBufInputStream(((HttpContent) req).content()),
        req.headers().getInt(HttpHeaderNames.CONTENT_LENGTH, -1));
  }

  /* **********************************************************************************************
   * Response methods:
   * **********************************************************************************************
   */

  @Nonnull @Override public Context statusCode(int statusCode) {
    this.status = HttpResponseStatus.valueOf(statusCode);
    return this;
  }

  @Override public final Context type(String contentType, String charset) {
    if (charset == null) {
      setHeaders.set(CONTENT_TYPE, contentType);
    } else {
      setHeaders.set(CONTENT_TYPE, contentType + ";charset=" + charset);
    }
    return this;
  }

  @Nonnull @Override public Context length(long length) {
    setHeaders.set(CONTENT_LENGTH, length);
    return this;
  }

  @Nonnull @Override public Context sendText(@Nonnull String data) {
    String result = (String) fireAfter(data);
    return sendByteBuf(copiedBuffer(result, UTF_8));
  }

  @Override public final Context sendText(String data, Charset charset) {
    String result = (String) fireAfter(data);
    return sendByteBuf(copiedBuffer(result, charset));
  }

  @Override public final Context sendBytes(byte[] data) {
    byte[] result = (byte[]) fireAfter(data);
    return sendByteBuf(wrappedBuffer(result));
  }

  @Override public final Context sendBytes(ByteBuffer data) {
    ByteBuffer result = (ByteBuffer) fireAfter(data);
    return sendByteBuf(wrappedBuffer(result));
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

  @Nonnull @Override public Context sendError(Throwable cause) {
    errorHandler.apply(this, cause);
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
      // rsp.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
      ctx.write(rsp, ctx.voidPromise());
    } else {
      ctx.write(rsp).addListener(CLOSE);
    }
    ctx.executor().execute(ctx::flush);
    // clean up
    destroy();
    return this;
  }

  public void destroy() {
    if (files != null) {
      // TODO: use a log
      files.forEach(throwingConsumer(Value.Upload::destroy).onFailure(x -> x.printStackTrace()));
    }
    release(req);
  }

  private Value.Upload register(Value.Upload upload) {
    if (this.files == null) {
      this.files = new ArrayList<>();
    }
    this.files.add(upload);
    return upload;
  }

  private void decodeForm(HttpRequest req, InterfaceHttpPostRequestDecoder decoder,
      Value.Object form) {
    try {
      while (decoder.hasNext()) {
        HttpData next = (HttpData) decoder.next();
        if (next.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
          form.put(next.getName(), register(new NettyUpload(next.getName(), (FileUpload) next)));
        } else {
          form.put(next.getName(), next.getString(UTF_8));
          next.release();
        }
      }
    } catch (HttpPostRequestDecoder.EndOfDataDecoderException x) {
      // ignore, silly netty
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    } finally {
      release(req);
    }
  }

  private static void release(HttpRequest req) {
    if (req instanceof ReferenceCounted) {
      ReferenceCounted ref = (ReferenceCounted) req;
      if (ref.refCnt() > 0) {
        ref.release();
      }
    }
  }
}

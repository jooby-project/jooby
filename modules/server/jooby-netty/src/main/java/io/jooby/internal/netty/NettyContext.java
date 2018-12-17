/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import io.jooby.*;
import io.jooby.FileUpload;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.ReferenceCounted;
import io.jooby.Throwing;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executor;

import static io.jooby.Throwing.throwingConsumer;
import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.channel.ChannelFutureListener.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;

public class NettyContext extends BaseContext {
  final HttpHeaders setHeaders = new DefaultHttpHeaders(false);
  private final Route.RootErrorHandler errorHandler;
  private final ChannelHandlerContext ctx;
  private final Path tmpdir;
  private HttpRequest req;
  private final String path;
  private final Executor executor;
  private HttpResponseStatus status = HttpResponseStatus.OK;
  private boolean keepAlive;
  private boolean responseStarted;
  private QueryString query;
  private Formdata form;
  private Multipart multipart;
  private List<FileUpload> files;
  private Value.Object headers;
  private Map<String, String> pathMap = Collections.emptyMap();

  public NettyContext(ChannelHandlerContext ctx, Executor executor,
      HttpRequest req, Route.RootErrorHandler errorHandler, Path tmpdir, String path) {
    this.path = path;
    this.ctx = ctx;
    this.req = req;
    this.executor = executor;
    this.keepAlive = isKeepAlive(req);
    this.tmpdir = tmpdir;
    this.errorHandler = errorHandler;
  }

  @Override public String name() {
    return "netty";
  }

  /* **********************************************************************************************
   * Request methods:
   * **********************************************************************************************
   */

  @Nonnull @Override public String method() {
    return req.method().asciiName().toUpperCase().toString();
  }

  @Nonnull @Override public Route route() {
    return route;
  }

  @Nonnull @Override public Context route(@Nonnull Route route) {
    this.route = route;
    return this;
  }

  @Nonnull @Override public final String pathString() {
    return path;
  }

  @Nonnull @Override public Map<String, String> pathMap() {
    return pathMap;
  }

  @Nonnull @Override public Context pathMap(@Nonnull Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
  }

  @Override public final boolean isInIoThread() {
    return ctx.channel().eventLoop().inEventLoop();
  }

  @Nonnull @Override public Context dispatch(@Nonnull Runnable action) {
    return dispatch(executor, action);
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

  @Nonnull @Override public Formdata form() {
    if (form == null) {
      form = new Formdata();
      decodeForm(req, new HttpPostStandardRequestDecoder(req), form);
    }
    return form;
  }

  @Nonnull @Override public Multipart multipart() {
    requireBlocking();
    if (multipart == null) {
      multipart = new Multipart();
      form = multipart;
      HttpDataFactory factory = new DefaultHttpDataFactory(Server._16KB);
      decodeForm(req,
          new HttpPostMultipartRequestDecoder(factory, req, StandardCharsets.UTF_8),
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

  @Nonnull @Override public Context header(@Nonnull String name, @Nonnull String value) {
    setHeaders.set(name, value);
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
    return sendByteBuf(copiedBuffer(data, UTF_8));
  }

  @Override public final Context sendText(String data, Charset charset) {
    return sendByteBuf(copiedBuffer(data, charset));
  }

  @Override public final Context sendBytes(byte[] data) {
    return sendByteBuf(wrappedBuffer(data));
  }

  @Override public final Context sendBytes(ByteBuffer data) {
    return sendByteBuf(wrappedBuffer(data));
  }

  @Override public boolean isResponseStarted() {
    return responseStarted;
  }

  @Nonnull @Override public Context sendStatusCode(int statusCode) {
    responseStarted = true;
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
      rsp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
      ctx.write(rsp, ctx.voidPromise());
    } else {
      ctx.write(rsp).addListener(CLOSE);
    }
    ctx.executor().execute(() -> ctx.flush());
    // TODO: move destroy inside a Write listener
    destroy();
    return this;
  }

  public void destroy() {
    if (files != null) {
      // TODO: use a log
      files.forEach(throwingConsumer(FileUpload::destroy));
    }
    release(req);
  }

  private FileUpload register(FileUpload upload) {
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
          form.put(next.getName(),
              register(new NettyFileUpload(tmpdir, next.getName(),
                  (io.netty.handler.codec.http.multipart.FileUpload) next)));
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

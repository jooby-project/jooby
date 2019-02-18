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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.ReferenceCounted;
import io.jooby.Throwing;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Executor;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.RANGE;
import static io.netty.handler.codec.http.HttpHeaderNames.TRANSFER_ENCODING;
import static io.netty.handler.codec.http.HttpHeaderValues.CHUNKED;
import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;
import static java.nio.charset.StandardCharsets.UTF_8;

public class NettyContext implements Context, ChannelFutureListener {

  private static final HttpHeaders NO_TRAILING = new DefaultHttpHeaders(false);
  private final HttpHeaders setHeaders = new DefaultHttpHeaders(false);
  private final int bufferSize;
  InterfaceHttpPostRequestDecoder decoder;
  private Router router;
  private Route route;
  private ChannelHandlerContext ctx;
  private HttpRequest req;
  private String path;
  private HttpResponseStatus status = HttpResponseStatus.OK;
  private boolean responseStarted;
  private QueryString query;
  private Formdata form;
  private Multipart multipart;
  private List<FileUpload> files;
  private Value.Object headers;
  private Map<String, String> pathMap = Collections.EMPTY_MAP;
  private Map<String, Object> locals = Collections.EMPTY_MAP;
  private MediaType responseType;

  public NettyContext(ChannelHandlerContext ctx, HttpRequest req, Router router, String path,
      int bufferSize) {
    this.path = path;
    this.ctx = ctx;
    this.req = req;
    this.router = router;
    this.bufferSize = bufferSize;
  }

  @Nonnull @Override public Router router() {
    return router;
  }

  @Override public String name() {
    return "netty";
  }

  /* **********************************************************************************************
   * Request methods:
   * **********************************************************************************************
   */

  @Nonnull @Override public Map<String, Object> attributes() {
    return locals;
  }

  @Nullable @Override public <T> T attribute(String name) {
    return (T) locals.get(name);
  }

  @Nonnull @Override public Context attribute(@Nonnull String name, @Nonnull Object value) {
    if (locals == Collections.EMPTY_MAP) {
      locals = new HashMap<>();
    }
    locals.put(name, value);
    return this;
  }

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

  @Nonnull @Override public Context setPathMap(@Nonnull Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
  }

  @Override public final boolean isInIoThread() {
    return ctx.channel().eventLoop().inEventLoop();
  }

  @Nonnull @Override public Context dispatch(@Nonnull Runnable action) {
    return dispatch(router.worker(), action);
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
      decodeForm(req, form);
    }
    return form;
  }

  @Nonnull @Override public Multipart multipart() {
    if (multipart == null) {
      multipart = new Multipart();
      form = multipart;
      decodeForm(req, multipart);
    }
    return multipart;
  }

  @Nonnull @Override public Value header(@Nonnull String name) {
    return Value.create(name, req.headers().getAll(name));
  }

  @Nonnull @Override public String remoteAddress() {
    InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
    return remoteAddress.getAddress().getHostAddress();
  }

  @Nonnull @Override public String protocol() {
    return req.protocolVersion().text();
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
    if (decoder != null && decoder.hasNext()) {
      return new NettyBody((HttpData) decoder.next(), HttpUtil.getContentLength(req, -1L));
    }
    return Body.empty();
  }

  /* **********************************************************************************************
   * Response methods:
   * **********************************************************************************************
   */

  @Nonnull @Override public StatusCode statusCode() {
    return StatusCode.valueOf(this.status.code());
  }

  @Nonnull @Override public Context statusCode(int statusCode) {
    this.status = HttpResponseStatus.valueOf(statusCode);
    return this;
  }

  @Nonnull @Override public Context setHeader(@Nonnull String name, @Nonnull String value) {
    setHeaders.set(name, value);
    return this;
  }

  @Nonnull @Override public MediaType responseContentType() {
    return responseType == null ? MediaType.text : responseType;
  }

  @Nonnull @Override public Context setDefaultContentType(@Nonnull MediaType contentType) {
    if (responseType == null) {
      setContentType(contentType, contentType.charset());
    }
    return this;
  }

  @Override public final Context setContentType(MediaType contentType, Charset charset) {
    this.responseType = contentType;
    setHeaders.set(CONTENT_TYPE, contentType.toContentTypeHeader(charset));
    return this;
  }

  @Nonnull @Override public Context setContentLength(long length) {
    setHeaders.set(CONTENT_LENGTH, length);
    return this;
  }

  @Nonnull @Override public PrintWriter responseWriter(MediaType type, Charset charset) {
    setContentType(type, charset);

    return new PrintWriter(new NettyWriter(newOutputStream(), charset));
  }

  @Nonnull @Override public Sender responseSender() {
    responseStarted = true;
    prepareChunked();
    ctx.write(new DefaultHttpResponse(req.protocolVersion(), status, setHeaders));
    return new NettySender(this, ctx);
  }

  @Nonnull @Override public OutputStream responseStream() {
    return newOutputStream();
  }

  @Nonnull @Override public Context sendString(@Nonnull String data) {
    return sendBytes(copiedBuffer(data, UTF_8));
  }

  @Override public final Context sendString(String data, Charset charset) {
    return sendBytes(copiedBuffer(data, charset));
  }

  @Override public final Context sendBytes(byte[] data) {
    return sendBytes(wrappedBuffer(data));
  }

  @Override public final Context sendBytes(ByteBuffer data) {
    return sendBytes(wrappedBuffer(data));
  }

  @Nonnull @Override public Context sendBytes(@Nonnull ByteBuf data) {
    responseStarted = true;
    setHeaders.set(CONTENT_LENGTH, data.readableBytes());
    ctx.writeAndFlush(
        new DefaultFullHttpResponse(HTTP_1_1, status, data, setHeaders, NO_TRAILING))
        .addListener(this);
    return this;
  }

  @Nonnull @Override public Context sendStream(@Nonnull InputStream in) {
    if (in instanceof FileInputStream) {
      // use channel
      return sendFile(((FileInputStream) in).getChannel());
    }
    try {
      prepareChunked();
      long len = responseLength();
      ChunkedInput chunkedStream;
      if (len > 0) {
        ByteRange range = ByteRange.parse(req.headers().get(RANGE)).apply(this, len);
        in.skip(range.start);
        chunkedStream = new ChunkedLimitedStream(in, bufferSize, range.end);
      } else {
        chunkedStream = new ChunkedStream(in, bufferSize);
      }

      DefaultHttpResponse rsp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, setHeaders);
      responseStarted = true;
      ctx.channel().eventLoop().execute(() -> {
        // Headers
        ctx.write(rsp);
        // Body
        ctx.write(chunkedStream);
        // Finish
        ctx.writeAndFlush(EMPTY_LAST_CONTENT).addListener(this);
      });
      return this;
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  @Nonnull @Override public Context sendFile(@Nonnull FileChannel file) {
    try {
      long len = file.size();
      setHeaders.set(CONTENT_LENGTH, len);

      ByteRange range = ByteRange.parse(req.headers().get(RANGE)).apply(this, len);

      DefaultHttpResponse rsp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status, setHeaders);
      responseStarted = true;
      ctx.channel().eventLoop().execute(() -> {
        // Headers
        ctx.write(rsp);
        // Body
        ctx.write(new DefaultFileRegion(file, range.start, range.end));
        // Finish
        ctx.writeAndFlush(EMPTY_LAST_CONTENT).addListener(this);
      });
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
    return this;
  }

  @Override public boolean isResponseStarted() {
    return responseStarted;
  }

  @Nonnull @Override public Context sendStatusCode(int statusCode) {
    responseStarted = true;
    setHeaders.set(CONTENT_LENGTH, 0);
    DefaultFullHttpResponse rsp = new DefaultFullHttpResponse(HTTP_1_1,
        HttpResponseStatus.valueOf(statusCode), Unpooled.EMPTY_BUFFER, setHeaders, NO_TRAILING);
    ctx.writeAndFlush(rsp).addListener(this);
    return this;
  }

  @Override public void operationComplete(ChannelFuture future) {
    boolean keepAlive = isKeepAlive(req);
    try {
      destroy(future.cause());
    } finally {
      if (!keepAlive) {
        future.channel().close();
      }
    }
  }

  private NettyOutputStream newOutputStream() {
    prepareChunked();
    return new NettyOutputStream(ctx, bufferSize,
        new DefaultHttpResponse(req.protocolVersion(), status, setHeaders), this);
  }

  void destroy(Throwable cause) {
    Logger log = router.log();
    if (cause != null) {
      if (Server.connectionLost(cause)) {
        log.debug("exception found while sending response {} {}", method(), pathString(), cause);
      } else {
        log.error("exception found while sending response {} {}", method(), pathString(), cause);
      }
    }
    if (files != null) {
      for (FileUpload file : files) {
        try {
          file.destroy();
        } catch (Exception x) {
          log.debug("file upload destroy resulted in exception", x);
        }
      }
      files = null;
    }
    if (decoder != null) {
      try {
        decoder.destroy();
      } catch (Exception x) {
        log.debug("body decoder destroy resulted in exception", x);
      }
      decoder = null;
    }
    release(req);
    this.route = null;
    this.ctx = null;
    this.req = null;
    this.router = null;
  }

  private FileUpload register(FileUpload upload) {
    if (this.files == null) {
      this.files = new ArrayList<>();
    }
    this.files.add(upload);
    return upload;
  }

  private void decodeForm(HttpRequest req, Value.Object form) {
    try {
      while (decoder.hasNext()) {
        HttpData next = (HttpData) decoder.next();
        if (next.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
          form.put(next.getName(),
              register(new NettyFileUpload(router.tmpdir(), next.getName(),
                  (io.netty.handler.codec.http.multipart.FileUpload) next)));
        } else {
          form.put(next.getName(), next.getString(UTF_8));
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

  private long responseLength() {
    String len = setHeaders.get(CONTENT_LENGTH);
    return len == null ? -1 : Long.parseLong(len);
  }

  private void prepareChunked() {
    // remove flusher, doesn't play well with streaming/chunked responses
    ctx.pipeline().remove("flusher");
    if (!setHeaders.contains(CONTENT_LENGTH)) {
      setHeaders.set(TRANSFER_ENCODING, CHUNKED);
    }
  }
}

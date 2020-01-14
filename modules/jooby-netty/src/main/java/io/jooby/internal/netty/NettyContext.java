/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import com.typesafe.config.Config;
import io.jooby.Body;
import io.jooby.ByteRange;
import io.jooby.CompletionListeners;
import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.DefaultContext;
import io.jooby.FileUpload;
import io.jooby.Formdata;
import io.jooby.MediaType;
import io.jooby.Multipart;
import io.jooby.QueryString;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.RouterOption;
import io.jooby.Sender;
import io.jooby.Server;
import io.jooby.ServerSentEmitter;
import io.jooby.Session;
import io.jooby.SessionStore;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;
import io.jooby.Value;
import io.jooby.ValueNode;
import io.jooby.WebSocket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.handler.stream.ChunkedNioStream;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCounted;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.RANGE;
import static io.netty.handler.codec.http.HttpHeaderNames.SET_COOKIE;
import static io.netty.handler.codec.http.HttpHeaderNames.TRANSFER_ENCODING;
import static io.netty.handler.codec.http.HttpHeaderValues.CHUNKED;
import static io.netty.handler.codec.http.HttpUtil.isKeepAlive;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;
import static java.nio.charset.StandardCharsets.UTF_8;

public class NettyContext implements DefaultContext, ChannelFutureListener {

  private static final HttpHeaders NO_TRAILING = EmptyHttpHeaders.INSTANCE;
  DefaultHttpHeaders setHeaders = new DefaultHttpHeaders(true);
  private final int bufferSize;
  InterfaceHttpPostRequestDecoder decoder;
  private Router router;
  private Route route;
  ChannelHandlerContext ctx;
  private HttpRequest req;
  private final String path;
  private HttpResponseStatus status = HttpResponseStatus.OK;
  private boolean responseStarted;
  private QueryString query;
  private Formdata form;
  private Multipart multipart;
  private List<FileUpload> files;
  private ValueNode headers;
  private Map<String, String> pathMap = Collections.EMPTY_MAP;
  private MediaType responseType;
  private Map<String, Object> attributes = new HashMap<>();
  private long contentLength = -1;
  private boolean needsFlush;
  private Map<String, String> cookies;
  private Map<String, String> responseCookies;
  private Boolean resetHeadersOnError;
  NettyWebSocket webSocket;
  private final String method;
  private CompletionListeners listeners;

  public NettyContext(ChannelHandlerContext ctx, HttpRequest req, Router router, String path,
      int bufferSize) {
    this.path = path;
    this.ctx = ctx;
    this.req = req;
    this.router = router;
    this.bufferSize = bufferSize;
    this.method = req.method().name().toUpperCase();
  }

  @Nonnull @Override public Router getRouter() {
    return router;
  }

  /* **********************************************************************************************
   * Request methods:
   * **********************************************************************************************
   */

  @Nonnull @Override public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Nonnull @Override public String getMethod() {
    return method;
  }

  @Nonnull @Override public Route getRoute() {
    return route;
  }

  @Nonnull @Override public Context setRoute(@Nonnull Route route) {
    this.route = route;
    return this;
  }

  @Nonnull @Override public final String pathString() {
    return path;
  }

  @Nonnull @Override public String getRequestPath() {
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
    return dispatch(router.getWorker(), action);
  }

  @Override public Context dispatch(Executor executor, Runnable action) {
    executor.execute(action);
    return this;
  }

  @Nonnull @Override public Context detach(@Nonnull Route.Handler next) throws Exception {
    next.apply(this);
    return this;
  }

  @Nonnull @Override public QueryString query() {
    if (query == null) {
      String uri = req.uri();
      int q = uri.indexOf('?');
      query = QueryString.create(this, q >= 0 ? uri.substring(q + 1) : null);
    }
    return query;
  }

  @Nonnull @Override public Formdata form() {
    if (form == null) {
      form = Formdata.create(this);
      decodeForm(req, form);
    }
    return form;
  }

  @Nonnull @Override public Multipart multipart() {
    if (multipart == null) {
      multipart = Multipart.create(this);
      form = multipart;
      decodeForm(req, multipart);
    }
    return multipart;
  }

  @Nonnull @Override public Value header(@Nonnull String name) {
    return Value.create(this, name, req.headers().getAll(name));
  }

  @Nonnull @Override public String getRemoteAddress() {
    InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
    String hostAddress = remoteAddress.getAddress().getHostAddress();
    int i = hostAddress.lastIndexOf('%');
    return i > 0 ? hostAddress.substring(0, i) : hostAddress;
  }

  @Nonnull @Override public String getProtocol() {
    return req.protocolVersion().text();
  }

  @Nonnull @Override public String getScheme() {
    return ctx.pipeline().get("ssl") == null ? "http" : "https";
  }

  @Nonnull @Override public ValueNode header() {
    if (headers == null) {
      Map<String, Collection<String>> headerMap = new LinkedHashMap<>();
      HttpHeaders headers = req.headers();
      Set<String> names = headers.names();
      for (String name : names) {
        headerMap.put(name, headers.getAll(name));
      }
      this.headers = Value.hash(this, headerMap);
    }
    return headers;
  }

  @Nonnull @Override public Body body() {
    if (decoder != null && decoder.hasNext()) {
      return new NettyBody(this, (HttpData) decoder.next(), HttpUtil.getContentLength(req, -1L));
    }
    return Body.empty(this);
  }

  @Override public @Nonnull Map<String, String> cookieMap() {
    if (this.cookies == null) {
      this.cookies = Collections.emptyMap();
      String cookieString = req.headers().get(HttpHeaderNames.COOKIE);
      if (cookieString != null) {
        Set<io.netty.handler.codec.http.cookie.Cookie> cookies = ServerCookieDecoder.STRICT
            .decode(cookieString);
        if (cookies.size() > 0) {
          this.cookies = new LinkedHashMap<>(cookies.size());
          for (io.netty.handler.codec.http.cookie.Cookie it : cookies) {
            this.cookies.put(it.name(), it.value());
          }
        }
      }
    }
    return this.cookies;
  }

  @Nonnull @Override public Context onComplete(@Nonnull Route.Complete task) {
    if (listeners == null) {
      listeners = new CompletionListeners();
    }
    listeners.addListener(task);
    return this;
  }

  @Nonnull @Override public Context upgrade(WebSocket.Initializer handler) {
    try {
      responseStarted = true;
      String webSocketURL = getProtocol() + "://" + req.headers().get(HttpHeaderNames.HOST) + path;
      WebSocketDecoderConfig config = WebSocketDecoderConfig.newBuilder()
          .allowExtensions(true)
          .allowMaskMismatch(false)
          .withUTF8Validator(false)
          .maxFramePayloadLength(WebSocket.MAX_BUFFER_SIZE)
          .build();
      webSocket = new NettyWebSocket(this);
      handler.init(Context.readOnly(this), webSocket);
      DefaultFullHttpRequest fullHttpRequest = new DefaultFullHttpRequest(req.protocolVersion(),
          req.method(), req.uri(), Unpooled.EMPTY_BUFFER, req.headers(), EmptyHttpHeaders.INSTANCE);
      WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(webSocketURL,
          null, config);
      WebSocketServerHandshaker handshaker = factory.newHandshaker(fullHttpRequest);
      Channel channel = ctx.channel();
      handshaker.handshake(channel, fullHttpRequest, setHeaders, ctx.newPromise())
          .addListener(future -> {
            if (future.isSuccess()) {
              webSocket.fireConnect(null);
            } else {
              handshaker.close(channel, new CloseWebSocketFrame());
            }
          });
      Config conf = getRouter().getConfig();
      long timeout = conf.hasPath("websocket.idleTimeout")
          ? conf.getDuration("websocket.idleTimeout", TimeUnit.MINUTES)
          : 5;
      if (timeout > 0) {
        IdleStateHandler idle = new IdleStateHandler(timeout, 0, 0, TimeUnit.MINUTES);
        ctx.pipeline().addBefore("handler", "idle", idle);
      }
    } catch (Throwable x) {
      sendError(x);
    }
    return this;
  }

  @Nonnull @Override public Context upgrade(@Nonnull ServerSentEmitter.Handler handler) {
    responseStarted = true;
    ctx.writeAndFlush(new DefaultHttpResponse(HTTP_1_1, status, setHeaders));

    //    ctx.executor().execute(() -> {
    try {
      handler.handle(new NettyServerSentEmitter(this));
    } catch (Throwable x) {
      sendError(x);
    }
    //    });
    return this;
  }

  /* **********************************************************************************************
   * Response methods:
   * **********************************************************************************************
   */

  @Nonnull @Override public StatusCode getResponseCode() {
    return StatusCode.valueOf(this.status.code());
  }

  @Nonnull @Override public Context setResponseCode(int statusCode) {
    this.status = HttpResponseStatus.valueOf(statusCode);
    return this;
  }

  @Nonnull @Override public Context setResponseHeader(@Nonnull String name, @Nonnull String value) {
    setHeaders.set(name, value);
    return this;
  }

  @Nonnull @Override public Context removeResponseHeader(@Nonnull String name) {
    setHeaders.remove(name);
    return this;
  }

  @Nonnull @Override public Context removeResponseHeaders() {
    setHeaders.clear();
    return this;
  }

  @Nonnull @Override public MediaType getResponseType() {
    return responseType == null ? MediaType.text : responseType;
  }

  @Nonnull @Override public Context setDefaultResponseType(@Nonnull MediaType contentType) {
    if (responseType == null) {
      setResponseType(contentType, contentType.getCharset());
    }
    return this;
  }

  @Override public final Context setResponseType(MediaType contentType, Charset charset) {
    this.responseType = contentType;
    setHeaders.set(CONTENT_TYPE, contentType.toContentTypeHeader(charset));
    return this;
  }

  @Nonnull @Override public Context setResponseType(@Nonnull String contentType) {
    this.responseType = MediaType.valueOf(contentType);
    setHeaders.set(CONTENT_TYPE, contentType);
    return this;
  }

  @Nullable @Override public String getResponseHeader(@Nonnull String name) {
    return setHeaders.get(name);
  }

  @Nonnull @Override public Context setResponseLength(long length) {
    contentLength = length;
    setHeaders.set(CONTENT_LENGTH, Long.toString(length));
    return this;
  }

  @Override public long getResponseLength() {
    if (contentLength == -1) {
      return Long.parseLong(setHeaders.get(CONTENT_LENGTH, "-1"));
    }
    return contentLength;
  }

  @Nonnull public Context setResponseCookie(@Nonnull Cookie cookie) {
    if (responseCookies == null) {
      responseCookies = new HashMap<>();
    }
    cookie.setPath(cookie.getPath(getContextPath()));
    responseCookies.put(cookie.getName(), cookie.toCookieString());
    setHeaders.remove(SET_COOKIE);
    for (String cookieString : responseCookies.values()) {
      setHeaders.add(SET_COOKIE, cookieString);
    }
    return this;
  }

  @Nonnull @Override public PrintWriter responseWriter(MediaType type, Charset charset) {
    responseStarted = true;
    setResponseType(type, charset);

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

  @Nonnull @Override public Context send(@Nonnull String data) {
    return send(copiedBuffer(data, UTF_8));
  }

  @Override public final Context send(String data, Charset charset) {
    return send(copiedBuffer(data, charset));
  }

  @Override public final Context send(byte[] data) {
    return send(wrappedBuffer(data));
  }

  @Nonnull @Override public Context send(@Nonnull byte[]... data) {
    return send(Unpooled.wrappedBuffer(data));
  }

  @Nonnull @Override public Context send(@Nonnull ByteBuffer[] data) {
    return send(Unpooled.wrappedBuffer(data));
  }

  @Override public final Context send(ByteBuffer data) {
    return send(wrappedBuffer(data));
  }

  private Context send(@Nonnull ByteBuf data) {
    responseStarted = true;
    setHeaders.set(CONTENT_LENGTH, Long.toString(data.readableBytes()));
    DefaultFullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,
        data, setHeaders, NO_TRAILING);
    if (ctx.channel().eventLoop().inEventLoop()) {
      needsFlush = true;
      ctx.write(response, promise(this));
    } else {
      ctx.writeAndFlush(response, promise(this));
    }
    return this;
  }

  public void flush() {
    if (needsFlush) {
      needsFlush = false;
      ctx.flush();
      this.ctx = null;
      this.req = null;
      this.route = null;
      this.router = null;
      this.headers = null;
    }
  }

  @Nonnull @Override public Context send(@Nonnull ReadableByteChannel channel) {
    prepareChunked();
    DefaultHttpResponse rsp = new DefaultHttpResponse(HTTP_1_1, status, setHeaders);
    responseStarted = true;
    int bufferSize = contentLength > 0 ? (int) contentLength : this.bufferSize;
    ctx.channel().eventLoop().execute(() -> {
      // Headers
      ctx.write(rsp, ctx.voidPromise());
      // Body
      ctx.write(new ChunkedNioStream(channel, bufferSize), ctx.voidPromise());
      // Finish
      ctx.writeAndFlush(EMPTY_LAST_CONTENT, promise(this));
    });
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull InputStream in) {
    if (in instanceof FileInputStream) {
      // use channel
      return send(((FileInputStream) in).getChannel());
    }
    try {
      prepareChunked();
      long len = responseLength();
      ByteRange range = ByteRange.parse(req.headers().get(RANGE), len)
          .apply(this);
      ChunkedStream chunkedStream = new ChunkedStream(range.apply(in), bufferSize);

      DefaultHttpResponse rsp = new DefaultHttpResponse(HTTP_1_1, status, setHeaders);
      responseStarted = true;
      ctx.channel().eventLoop().execute(() -> {
        // Headers
        ctx.write(rsp, ctx.voidPromise());
        // Body
        ctx.write(chunkedStream, ctx.voidPromise());
        // Finish
        ctx.writeAndFlush(EMPTY_LAST_CONTENT, promise(this));
      });
      return this;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @Nonnull @Override public Context send(@Nonnull FileChannel file) {
    try {
      long len = file.size();
      setHeaders.set(CONTENT_LENGTH, Long.toString(len));

      ByteRange range = ByteRange.parse(req.headers().get(RANGE), len)
          .apply(this);

      DefaultHttpResponse rsp = new DefaultHttpResponse(HTTP_1_1, status, setHeaders);
      responseStarted = true;

      if (isSecure()) {
        prepareChunked();

        HttpChunkedInput chunkedInput = new HttpChunkedInput(
            new ChunkedNioFile(file, range.getStart(), range.getEnd(), bufferSize));

        ctx.channel().eventLoop().execute(() -> {
          // Headers
          ctx.write(rsp, ctx.voidPromise());
          // Body
          ctx.writeAndFlush(chunkedInput, promise(this));
        });
      } else {
        ctx.channel().eventLoop().execute(() -> {
          // Headers
          ctx.write(rsp, ctx.voidPromise());
          // Body
          ctx.write(new DefaultFileRegion(file, range.getStart(), range.getEnd()),
              ctx.voidPromise());
          // Finish
          ctx.writeAndFlush(EMPTY_LAST_CONTENT, promise(this));
        });
      }
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
    return this;
  }

  @Override public boolean isResponseStarted() {
    return responseStarted;
  }

  @Override public boolean getResetHeadersOnError() {
    return resetHeadersOnError == null
        ? getRouter().getRouterOptions().contains(RouterOption.RESET_HEADERS_ON_ERROR)
        : resetHeadersOnError.booleanValue();
  }

  @Override public Context setResetHeadersOnError(boolean value) {
    this.resetHeadersOnError = value;
    return this;
  }

  @Nonnull @Override public Context send(StatusCode statusCode) {
    setResponseCode(statusCode);
    responseStarted = true;
    if (!setHeaders.contains(CONTENT_LENGTH)) {
      setHeaders.set(CONTENT_LENGTH, "0");
    }
    DefaultFullHttpResponse rsp = new DefaultFullHttpResponse(HTTP_1_1,
        status, Unpooled.EMPTY_BUFFER, setHeaders,
        NO_TRAILING);
    ctx.writeAndFlush(rsp, promise(this));
    return this;
  }

  @Override public void operationComplete(ChannelFuture future) {
    try {
      fireCompleteEvent();
      ifSaveSession();
      destroy(future.cause());
    } finally {
      if (!isKeepAlive(req)) {
        future.channel().close();
      }
    }
  }

  private void fireCompleteEvent() {
    if (listeners != null) {
      listeners.run(this);
    }
  }

  private void ifSaveSession() {
    Session session = saveSession();
    if (session != null) {
      SessionStore store = router.getSessionStore();
      store.saveSession(this, session);
    }
  }

  private Session saveSession() {
    Session session = (Session) getAttributes().get(Session.NAME);
    if (session != null && (session.isNew() || session.isModify())) {
      return session;
    }
    return session;
  }

  private ChannelPromise promise(ChannelFutureListener listener) {
    if (pendingTasks()) {
      return ctx.newPromise().addListener(listener);
    }
    return ctx.voidPromise();
  }

  private boolean pendingTasks() {
    return (saveSession() != null) ||
        (listeners != null) ||
        (files != null && files.size() > 0) ||
        (decoder != null) ||
        shouldRelease(req);
  }

  void destroy(Throwable cause) {
    if (cause != null) {
      if (Server.connectionLost(cause)) {
        router.getLog()
            .debug("exception found while sending response {} {}", getMethod(), pathString(),
                cause);
      } else {
        router.getLog()
            .error("exception found while sending response {} {}", getMethod(), pathString(),
                cause);
      }
    }
    if (files != null) {
      for (FileUpload file : files) {
        try {
          file.destroy();
        } catch (Exception x) {
          router.getLog().debug("file upload destroy resulted in exception", x);
        }
      }
      files = null;
    }
    if (decoder != null) {
      try {
        decoder.destroy();
      } catch (Exception x) {
        router.getLog().debug("body decoder destroy resulted in exception", x);
      }
      decoder = null;
    }
    release(req);
  }

  private NettyOutputStream newOutputStream() {
    prepareChunked();
    return new NettyOutputStream(ctx, bufferSize,
        new DefaultHttpResponse(req.protocolVersion(), status, setHeaders), this);
  }

  private FileUpload register(FileUpload upload) {
    if (this.files == null) {
      this.files = new ArrayList<>();
    }
    this.files.add(upload);
    return upload;
  }

  private void decodeForm(HttpRequest req, Formdata form) {
    if (decoder == null) {
      // empty/bad form
      return;
    }
    try {
      while (decoder.hasNext()) {
        HttpData next = (HttpData) decoder.next();
        if (next.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
          ((Multipart) form).put(next.getName(),
              register(new NettyFileUpload(router.getTmpdir(),
                  (io.netty.handler.codec.http.multipart.FileUpload) next)));
        } else {
          form.put(next.getName(), next.getString(UTF_8));
        }
      }
    } catch (HttpPostRequestDecoder.EndOfDataDecoderException x) {
      // ignore, silly netty
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    } finally {
      release(req);
    }
  }

  private static void release(HttpRequest req) {
    if (shouldRelease(req)) {
      ReferenceCounted ref = (ReferenceCounted) req;
      ref.release();
    }
  }

  private static boolean shouldRelease(HttpRequest req) {
    if (req instanceof ReferenceCounted) {
      ReferenceCounted ref = (ReferenceCounted) req;
      return ref.refCnt() > 0;
    }
    return false;
  }

  private long responseLength() {
    String len = setHeaders.get(CONTENT_LENGTH);
    return len == null ? -1 : Long.parseLong(len);
  }

  private void prepareChunked() {
    // remove flusher, doesn't play well with streaming/chunked responses
    ChannelPipeline pipeline = ctx.pipeline();
    if (pipeline.get("chunker") == null) {
      pipeline.addAfter("encoder", "chunker", new ChunkedWriteHandler());
    }
    if (!setHeaders.contains(CONTENT_LENGTH)) {
      setHeaders.set(TRANSFER_ENCODING, CHUNKED);
    }
  }

  @Override public String toString() {
    return getMethod() + " " + pathString();
  }
}

/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

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
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

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
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import javax.net.ssl.SSLPeerUnverifiedException;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
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
import io.netty.handler.codec.http.FullHttpRequest;
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
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.handler.stream.ChunkedNioStream;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCounted;

public class NettyContext implements DefaultContext, ChannelFutureListener {

  private static final HttpHeaders NO_TRAILING = EmptyHttpHeaders.INSTANCE;
  private static final String STREAM_ID = "x-http2-stream-id";
  private final String streamId;
  DefaultHttpHeaders setHeaders = new DefaultHttpHeaders(true);
  private final int bufferSize;
  InterfaceHttpPostRequestDecoder decoder;
  private Router router;
  private Route route;
  ChannelHandlerContext ctx;
  private HttpRequest req;
  private String path;
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
  private String method;
  private CompletionListeners listeners;
  private String remoteAddress;
  private String host;
  private String scheme;
  private int port;

  public NettyContext(
      ChannelHandlerContext ctx, HttpRequest req, Router router, String path, int bufferSize) {
    this.path = path;
    this.ctx = ctx;
    this.req = req;
    this.router = router;
    this.bufferSize = bufferSize;
    this.method = req.method().name().toUpperCase();
    // Safe streamId for HTTP/2
    this.streamId = header(STREAM_ID).valueOrNull();
    ifStreamId(this.streamId);
  }

  boolean isHttpGet() {
    return this.method.length() == 3
        && this.method.charAt(0) == 'G'
        && this.method.charAt(1) == 'E'
        && this.method.charAt(2) == 'T';
  }

  @NonNull @Override
  public Router getRouter() {
    return router;
  }

  /* **********************************************************************************************
   * Request methods:
   * **********************************************************************************************
   */

  @NonNull @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @NonNull @Override
  public String getMethod() {
    return method;
  }

  @NonNull @Override
  public Context setMethod(@NonNull String method) {
    this.method = method.toUpperCase();
    return this;
  }

  @NonNull @Override
  public Route getRoute() {
    return route;
  }

  @NonNull @Override
  public Context setRoute(@NonNull Route route) {
    this.route = route;
    return this;
  }

  @NonNull @Override
  public String getRequestPath() {
    return path;
  }

  @NonNull @Override
  public Context setRequestPath(String path) {
    this.path = path;
    return this;
  }

  @NonNull @Override
  public Map<String, String> pathMap() {
    return pathMap;
  }

  @NonNull @Override
  public Context setPathMap(@NonNull Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
  }

  @Override
  public final boolean isInIoThread() {
    return ctx.channel().eventLoop().inEventLoop();
  }

  @NonNull @Override
  public Context dispatch(@NonNull Runnable action) {
    return dispatch(router.getWorker(), action);
  }

  @Override
  public Context dispatch(Executor executor, Runnable action) {
    executor.execute(action);
    return this;
  }

  @NonNull @Override
  public Context detach(@NonNull Route.Handler next) throws Exception {
    next.apply(this);
    return this;
  }

  @NonNull @Override
  public QueryString query() {
    if (query == null) {
      String uri = req.uri();
      int q = uri.indexOf('?');
      query = QueryString.create(this, q >= 0 ? uri.substring(q + 1) : null);
    }
    return query;
  }

  @NonNull @Override
  public Formdata form() {
    return multipart();
  }

  @NonNull @Override
  public Multipart multipart() {
    if (multipart == null) {
      multipart = Multipart.create(this);
      form = multipart;
      decodeForm(req, multipart);
    }
    return multipart;
  }

  @NonNull @Override
  public Value header(@NonNull String name) {
    return Value.create(this, name, req.headers().getAll(name));
  }

  @NonNull @Override
  public String getHost() {
    return host == null ? DefaultContext.super.getHost() : host;
  }

  @NonNull @Override
  public Context setHost(@NonNull String host) {
    this.host = host;
    return this;
  }

  @NonNull @Override
  public String getRemoteAddress() {
    if (this.remoteAddress == null) {
      InetSocketAddress inetAddress = (InetSocketAddress) ctx.channel().remoteAddress();
      if (inetAddress != null) {
        String hostAddress = inetAddress.getAddress().getHostAddress();
        int i = hostAddress.lastIndexOf('%');
        this.remoteAddress = i > 0 ? hostAddress.substring(0, i) : hostAddress;
        return remoteAddress;
      }
      return "";
    }
    return remoteAddress;
  }

  @NonNull @Override
  public Context setRemoteAddress(@NonNull String remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

  @NonNull @Override
  public String getProtocol() {
    if (ctx.pipeline().get("http2") == null) {
      return req.protocolVersion().text();
    } else {
      return "HTTP/2.0";
    }
  }

  @NonNull @Override
  public List<Certificate> getClientCertificates() {
    SslHandler sslHandler = (SslHandler) ctx.channel().pipeline().get("ssl");
    if (sslHandler != null) {
      try {
        return Arrays.asList(sslHandler.engine().getSession().getPeerCertificates());
      } catch (SSLPeerUnverifiedException x) {
        throw SneakyThrows.propagate(x);
      }
    }
    return new ArrayList<Certificate>();
  }

  @NonNull @Override
  public String getScheme() {
    if (scheme == null) {
      scheme = ctx.pipeline().get("ssl") == null ? "http" : "https";
    }
    return scheme;
  }

  @NonNull @Override
  public Context setScheme(@NonNull String scheme) {
    this.scheme = scheme;
    return this;
  }

  @Override
  public int getPort() {
    return port > 0 ? port : DefaultContext.super.getPort();
  }

  @NonNull @Override
  public Context setPort(int port) {
    this.port = port;
    return this;
  }

  @NonNull @Override
  public ValueNode header() {
    if (headers == null) {
      Map<String, Collection<String>> headerMap = new LinkedHashMap<>();
      HttpHeaders headers = req.headers();
      Set<String> names = headers.names();
      for (String name : names) {
        headerMap.put(name, headers.getAll(name));
      }
      this.headers = Value.headers(this, headerMap);
    }
    return headers;
  }

  @NonNull @Override
  public Body body() {
    if (decoder != null && decoder.hasNext()) {
      return new NettyBody(this, (HttpData) decoder.next(), HttpUtil.getContentLength(req, -1L));
    }
    return Body.empty(this);
  }

  @Override
  public @NonNull Map<String, String> cookieMap() {
    if (this.cookies == null) {
      this.cookies = Collections.emptyMap();
      String cookieString = req.headers().get(HttpHeaderNames.COOKIE);
      if (cookieString != null) {
        Set<io.netty.handler.codec.http.cookie.Cookie> cookies =
            ServerCookieDecoder.STRICT.decode(cookieString);
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

  @NonNull @Override
  public Context onComplete(@NonNull Route.Complete task) {
    if (listeners == null) {
      listeners = new CompletionListeners();
    }
    listeners.addListener(task);
    return this;
  }

  @NonNull @Override
  public Context upgrade(WebSocket.Initializer handler) {
    try {
      responseStarted = true;
      Config conf = getRouter().getConfig();
      int maxSize =
          conf.hasPath("websocket.maxSize")
              ? conf.getBytes("websocket.maxSize").intValue()
              : WebSocket.MAX_BUFFER_SIZE;
      String webSocketURL = getProtocol() + "://" + req.headers().get(HttpHeaderNames.HOST) + path;
      WebSocketDecoderConfig config =
          WebSocketDecoderConfig.newBuilder()
              .allowExtensions(true)
              .allowMaskMismatch(false)
              .withUTF8Validator(false)
              .maxFramePayloadLength(maxSize)
              .build();
      webSocket = new NettyWebSocket(this);
      handler.init(Context.readOnly(this), webSocket);
      FullHttpRequest webSocketRequest =
          new DefaultFullHttpRequest(
              req.protocolVersion(),
              req.method(),
              req.uri(),
              Unpooled.EMPTY_BUFFER,
              req.headers(),
              EmptyHttpHeaders.INSTANCE);
      WebSocketServerHandshakerFactory factory =
          new WebSocketServerHandshakerFactory(webSocketURL, null, config);
      WebSocketServerHandshaker handshaker = factory.newHandshaker(webSocketRequest);
      handshaker.handshake(ctx.channel(), webSocketRequest);
      webSocket.fireConnect();
      long timeout =
          conf.hasPath("websocket.idleTimeout")
              ? conf.getDuration("websocket.idleTimeout", MILLISECONDS)
              : MINUTES.toMillis(5);
      if (timeout > 0) {
        IdleStateHandler idle = new IdleStateHandler(timeout, 0, 0, MILLISECONDS);
        ctx.pipeline().addBefore("handler", "idle", idle);
      }
    } catch (Throwable x) {
      sendError(x);
    }
    return this;
  }

  @NonNull @Override
  public Context upgrade(@NonNull ServerSentEmitter.Handler handler) {
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

  @NonNull @Override
  public StatusCode getResponseCode() {
    return StatusCode.valueOf(this.status.code());
  }

  @NonNull @Override
  public Context setResponseCode(int statusCode) {
    this.status = HttpResponseStatus.valueOf(statusCode);
    return this;
  }

  @NonNull @Override
  public Context setResponseHeader(@NonNull String name, @NonNull String value) {
    setHeaders.set(name, value);
    return this;
  }

  @NonNull @Override
  public Context removeResponseHeader(@NonNull String name) {
    setHeaders.remove(name);
    return this;
  }

  @NonNull @Override
  public Context removeResponseHeaders() {
    setHeaders.clear();
    ifStreamId(this.streamId);
    return this;
  }

  @NonNull @Override
  public MediaType getResponseType() {
    return responseType == null ? MediaType.text : responseType;
  }

  @NonNull @Override
  public Context setDefaultResponseType(@NonNull MediaType contentType) {
    if (responseType == null) {
      setResponseType(contentType, contentType.getCharset());
    }
    return this;
  }

  @Override
  public final Context setResponseType(MediaType contentType, Charset charset) {
    this.responseType = contentType;
    setHeaders.set(CONTENT_TYPE, contentType.toContentTypeHeader(charset));
    return this;
  }

  @NonNull @Override
  public Context setResponseType(@NonNull String contentType) {
    this.responseType = MediaType.valueOf(contentType);
    setHeaders.set(CONTENT_TYPE, contentType);
    return this;
  }

  @Nullable @Override
  public String getResponseHeader(@NonNull String name) {
    return setHeaders.get(name);
  }

  @NonNull @Override
  public Context setResponseLength(long length) {
    contentLength = length;
    setHeaders.set(CONTENT_LENGTH, Long.toString(length));
    return this;
  }

  @Override
  public long getResponseLength() {
    if (contentLength == -1) {
      return Long.parseLong(setHeaders.get(CONTENT_LENGTH, "-1"));
    }
    return contentLength;
  }

  @NonNull public Context setResponseCookie(@NonNull Cookie cookie) {
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

  @NonNull @Override
  public PrintWriter responseWriter(MediaType type, Charset charset) {
    setResponseType(type, charset);

    return new PrintWriter(new NettyWriter(newOutputStream(), charset));
  }

  @NonNull @Override
  public Sender responseSender() {
    prepareChunked();
    ctx.write(new DefaultHttpResponse(req.protocolVersion(), status, setHeaders));
    return new NettySender(this, ctx);
  }

  @NonNull @Override
  public OutputStream responseStream() {
    return newOutputStream();
  }

  @NonNull @Override
  public Context send(@NonNull String data) {
    return send(copiedBuffer(data, UTF_8));
  }

  @Override
  public final Context send(String data, Charset charset) {
    return send(copiedBuffer(data, charset));
  }

  @Override
  public final Context send(byte[] data) {
    return send(wrappedBuffer(data));
  }

  @NonNull @Override
  public Context send(@NonNull byte[]... data) {
    return send(Unpooled.wrappedBuffer(data));
  }

  @NonNull @Override
  public Context send(@NonNull ByteBuffer[] data) {
    return send(Unpooled.wrappedBuffer(data));
  }

  @Override
  public final Context send(ByteBuffer data) {
    return send(wrappedBuffer(data));
  }

  private Context send(@NonNull ByteBuf data) {
    try {
      responseStarted = true;
      setHeaders.set(CONTENT_LENGTH, Long.toString(data.readableBytes()));
      DefaultFullHttpResponse response =
          new DefaultFullHttpResponse(HTTP_1_1, status, data, setHeaders, NO_TRAILING);
      if (ctx.channel().eventLoop().inEventLoop()) {
        needsFlush = true;
        ctx.write(response, promise(this));
      } else {
        ctx.writeAndFlush(response, promise(this));
      }
      return this;
    } finally {
      requestComplete();
    }
  }

  public void flush() {
    if (needsFlush) {
      needsFlush = false;
      ctx.flush();
    }
  }

  @NonNull @Override
  public Context send(@NonNull ReadableByteChannel channel) {
    try {
      prepareChunked();
      DefaultHttpResponse rsp = new DefaultHttpResponse(HTTP_1_1, status, setHeaders);
      int bufferSize = contentLength > 0 ? (int) contentLength : this.bufferSize;
      ctx.channel()
          .eventLoop()
          .execute(
              () -> {
                // Headers
                ctx.write(rsp, ctx.voidPromise());
                // Body
                ctx.write(new ChunkedNioStream(channel, bufferSize), ctx.voidPromise());
                // Finish
                ctx.writeAndFlush(EMPTY_LAST_CONTENT, promise(this));
              });
      return this;
    } finally {
      requestComplete();
    }
  }

  @NonNull @Override
  public Context send(@NonNull InputStream in) {
    if (in instanceof FileInputStream) {
      // use channel
      return send(((FileInputStream) in).getChannel());
    }
    try {
      long len = responseLength();
      ByteRange range = ByteRange.parse(req.headers().get(RANGE), len).apply(this);
      prepareChunked();
      ChunkedStream chunkedStream = new ChunkedStream(range.apply(in), bufferSize);

      DefaultHttpResponse rsp = new DefaultHttpResponse(HTTP_1_1, status, setHeaders);
      responseStarted = true;
      ctx.channel()
          .eventLoop()
          .execute(
              () -> {
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
    } finally {
      requestComplete();
    }
  }

  @NonNull @Override
  public Context send(@NonNull FileChannel file) {
    try {
      long len = file.size();
      setHeaders.set(CONTENT_LENGTH, Long.toString(len));

      ByteRange range = ByteRange.parse(req.headers().get(RANGE), len).apply(this);

      DefaultHttpResponse rsp = new DefaultHttpResponse(HTTP_1_1, status, setHeaders);
      responseStarted = true;

      if (isSecure() || isGzip()) {
        prepareChunked();

        HttpChunkedInput chunkedInput =
            new HttpChunkedInput(
                new ChunkedNioFile(file, range.getStart(), range.getEnd(), bufferSize));

        ctx.channel()
            .eventLoop()
            .execute(
                () -> {
                  // Headers
                  ctx.write(rsp, ctx.voidPromise());
                  // Body
                  ctx.writeAndFlush(chunkedInput, promise(this));
                });
      } else {
        ctx.channel()
            .eventLoop()
            .execute(
                () -> {
                  // Headers
                  ctx.write(rsp, ctx.voidPromise());
                  // Body
                  ctx.write(
                      new DefaultFileRegion(file, range.getStart(), range.getEnd()),
                      ctx.voidPromise());
                  // Finish
                  ctx.writeAndFlush(EMPTY_LAST_CONTENT, promise(this));
                });
      }
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    } finally {
      requestComplete();
    }
    return this;
  }

  @Override
  public boolean isResponseStarted() {
    return responseStarted;
  }

  @Override
  public boolean getResetHeadersOnError() {
    return resetHeadersOnError == null
        ? getRouter().getRouterOptions().contains(RouterOption.RESET_HEADERS_ON_ERROR)
        : resetHeadersOnError.booleanValue();
  }

  @Override
  public Context setResetHeadersOnError(boolean value) {
    this.resetHeadersOnError = value;
    return this;
  }

  @NonNull @Override
  public Context send(StatusCode statusCode) {
    try {
      setResponseCode(statusCode);
      responseStarted = true;
      if (!setHeaders.contains(CONTENT_LENGTH)) {
        setHeaders.set(CONTENT_LENGTH, "0");
      }
      DefaultFullHttpResponse rsp =
          new DefaultFullHttpResponse(
              HTTP_1_1, status, Unpooled.EMPTY_BUFFER, setHeaders, NO_TRAILING);
      ctx.writeAndFlush(rsp, promise(this));
      return this;
    } finally {
      requestComplete();
    }
  }

  void requestComplete() {
    fireCompleteEvent();
    ifSaveSession();
  }

  @Override
  public void operationComplete(ChannelFuture future) {
    try {
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
    Session session = getSession();
    if (session != null) {
      SessionStore store = router.getSessionStore();
      store.saveSession(this, session);
    }
  }

  private Session getSession() {
    return (Session) getAttributes().get(Session.NAME);
  }

  private ChannelPromise promise(ChannelFutureListener listener) {
    if (pendingTasks()) {
      return ctx.newPromise().addListener(listener);
    }
    return ctx.voidPromise();
  }

  private boolean pendingTasks() {
    return (getSession() != null)
        || (listeners != null)
        || (files != null && files.size() > 0)
        || (decoder != null)
        || shouldRelease(req);
  }

  void destroy(Throwable cause) {
    if (cause != null) {
      if (Server.connectionLost(cause)) {
        router
            .getLog()
            .debug(
                "exception found while sending response {} {}",
                getMethod(),
                getRequestPath(),
                cause);
      } else {
        router
            .getLog()
            .error(
                "exception found while sending response {} {}",
                getMethod(),
                getRequestPath(),
                cause);
      }
    }
    if (files != null) {
      for (FileUpload file : files) {
        try {
          file.close();
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
    return new NettyOutputStream(
        this, ctx, bufferSize, new DefaultHttpResponse(req.protocolVersion(), status, setHeaders));
  }

  private FileUpload register(FileUpload upload) {
    if (this.files == null) {
      this.files = new ArrayList<>();
    }
    this.files.add(upload);
    return upload;
  }

  private void decodeForm(HttpRequest req, Formdata form) {
    if (decoder == null || decoder instanceof HttpRawPostRequestDecoder) {
      // empty/bad form
      return;
    }
    try {
      while (decoder.hasNext()) {
        HttpData next = (HttpData) decoder.next();
        if (next.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
          ((Multipart) form)
              .put(
                  next.getName(),
                  register(
                      new NettyFileUpload(
                          router.getTmpdir(),
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
    responseStarted = true;
    // remove flusher, doesn't play well with streaming/chunked responses
    ChannelPipeline pipeline = ctx.pipeline();
    if (pipeline.get("chunker") == null) {
      String base =
          Stream.of("compressor", "codec", "http2")
              .filter(name -> pipeline.get(name) != null)
              .findFirst()
              .orElseThrow(
                  () -> new IllegalStateException("No available handler for chunk writer"));
      pipeline.addAfter(base, "chunker", new ChunkedWriteHandler());
    }
    if (!setHeaders.contains(CONTENT_LENGTH)) {
      setHeaders.set(TRANSFER_ENCODING, CHUNKED);
    }
  }

  @Override
  public String toString() {
    return getMethod() + " " + getRequestPath();
  }

  /**
   * Set stream ID response header.
   *
   * @param streamId Stream ID or null.
   */
  private void ifStreamId(String streamId) {
    if (streamId != null && streamId.length() > 0) {
      setResponseHeader(STREAM_ID, streamId);
    }
  }

  private boolean isGzip() {
    return getRouter().getServerOptions().getCompressionLevel() != null;
  }
}

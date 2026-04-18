/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import static io.jooby.internal.netty.NettyHeadersFactory.HEADERS;
import static io.jooby.internal.netty.NettyString.CONTENT_LENGTH;
import static io.jooby.internal.netty.NettyString.ZERO;
import static io.netty.buffer.Unpooled.wrappedBuffer;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.RANGE;
import static io.netty.handler.codec.http.HttpHeaderNames.SET_COOKIE;
import static io.netty.handler.codec.http.HttpHeaderNames.TRANSFER_ENCODING;
import static io.netty.handler.codec.http.HttpHeaderValues.CHUNKED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static io.netty.handler.codec.http.LastHttpContent.EMPTY_LAST_CONTENT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.cert.Certificate;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import io.jooby.*;
import io.jooby.Cookie;
import io.jooby.output.Output;
import io.jooby.value.Value;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.codec.http.websocketx.WebSocketDecoderConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;
import io.netty.handler.stream.ChunkedNioStream;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.IllegalReferenceCountException;

public class NettyContext implements DefaultContext {

  private static class DestroyDecoder implements ChannelFutureListener {

    private final Logger log;

    private InterfaceHttpPostRequestDecoder decoder;

    public DestroyDecoder(Logger log, InterfaceHttpPostRequestDecoder decoder) {
      this.log = log;
      this.decoder = decoder;
    }

    @Override
    public void operationComplete(ChannelFuture future) {
      try {
        decoder.destroy();
      } catch (IllegalReferenceCountException ex) {
        log.trace("decoder was released already", ex);
      } catch (Exception ex) {
        log.debug("body decoder destroy resulted in exception", ex);
      }
      decoder = null;
    }
  }

  private static class DeleteFileTask implements ChannelFutureListener {

    private final Logger log;
    private final String filePath;
    private final SneakyThrows.Runnable deleteTask;

    public DeleteFileTask(Logger log, String filePath, SneakyThrows.Runnable deleteTask) {
      this.log = log;
      this.filePath = filePath;
      this.deleteTask = deleteTask;
    }

    @Override
    public void operationComplete(ChannelFuture future) {
      try {
        deleteTask.run();
      } catch (Exception ex) {
        log.debug("deletion of {} resulted in exception", filePath, ex);
      }
    }
  }

  private static final HttpHeaders NO_TRAILING = EmptyHttpHeaders.INSTANCE;
  private static final String STREAM_ID = "x-http2-stream-id";

  private String streamId;
  HeadersMultiMap setHeaders = HEADERS.newHeaders();
  private int bufferSize;
  InterfaceHttpPostRequestDecoder decoder;
  DefaultHttpDataFactory httpDataFactory;
  private Router router;
  private Route route;
  ChannelHandlerContext ctx;
  private HttpRequest req;
  private String path;
  private HttpResponseStatus status = HttpResponseStatus.OK;
  private boolean responseStarted;
  private QueryString query;
  private Formdata formdata;
  private Value headers;
  private Map<String, String> pathMap = Collections.EMPTY_MAP;
  private MediaType responseType;
  private Map<String, Object> attributes;
  private long contentLength = -1;
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
  NettyHandler connection;
  private ChannelPromise responsePromise;

  public NettyContext(
      NettyHandler connection,
      ChannelHandlerContext ctx,
      HttpRequest req,
      Router router,
      String path,
      int bufferSize,
      boolean http2) {
    this.connection = connection;
    this.path = path;
    this.ctx = ctx;
    this.req = req;
    this.router = router;
    this.bufferSize = bufferSize;
    this.method = req.method().name();
    if (http2) {
      // Save streamId for HTTP/2
      this.streamId = req.headers().get(STREAM_ID);
      ifStreamId(this.streamId);
    }
    if (!HttpUtil.isKeepAlive(req)) {
      setHeaders.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
      getOrCreateResponsePromise().addListener(ChannelFutureListener.CLOSE);
    }
  }

  @Override
  public Router getRouter() {
    return router;
  }

  /* **********************************************************************************************
   * Request methods:
   * **********************************************************************************************
   */

  @Override
  public Map<String, Object> getAttributes() {
    if (attributes == null) {
      attributes = new HashMap<>();
    }
    return attributes;
  }

  @Override
  public String getMethod() {
    return method;
  }

  @Override
  public Context setMethod(String method) {
    this.method = method.toUpperCase();
    return this;
  }

  @Override
  public Route getRoute() {
    return route;
  }

  @Override
  public Context setRoute(Route route) {
    this.route = route;
    return this;
  }

  @Override
  public String getRequestPath() {
    return path;
  }

  @Override
  public Context setRequestPath(String path) {
    this.path = path;
    return this;
  }

  @Override
  public Map<String, String> pathMap() {
    return pathMap;
  }

  @Override
  public Context setPathMap(Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
  }

  @Override
  public final boolean isInIoThread() {
    return ctx.channel().eventLoop().inEventLoop();
  }

  @Override
  public Context dispatch(Runnable action) {
    return dispatch(router.getWorker(), action);
  }

  @Override
  public Context dispatch(Executor executor, Runnable action) {
    executor.execute(action);
    return this;
  }

  @Override
  public QueryString query() {
    if (query == null) {
      String uri = req.uri();
      int q = uri.indexOf('?');
      query = QueryString.create(getValueFactory(), q >= 0 ? uri.substring(q + 1) : null);
    }
    return query;
  }

  @Override
  public Formdata form() {
    if (formdata == null) {
      formdata = Formdata.create(getValueFactory());
      decodeForm(formdata);
    }
    return formdata;
  }

  @Override
  public Value header(String name) {
    return Value.create(getValueFactory(), name, req.headers().getAll(name));
  }

  @Override
  public String getHost() {
    return host == null ? DefaultContext.super.getHost() : host;
  }

  @Override
  public Context setHost(String host) {
    this.host = host;
    return this;
  }

  @Override
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

  @Override
  public Context setRemoteAddress(String remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

  @Override
  public String getProtocol() {
    if (ctx.pipeline().get("http2") == null) {
      return req.protocolVersion().text();
    } else {
      return "HTTP/2.0";
    }
  }

  @Override
  public List<Certificate> getClientCertificates() {
    var sslHandler = ssl();
    if (sslHandler != null) {
      try {
        return List.of(sslHandler.engine().getSession().getPeerCertificates());
      } catch (SSLPeerUnverifiedException x) {
        throw SneakyThrows.propagate(x);
      }
    }
    return Collections.emptyList();
  }

  @Override
  public String getScheme() {
    if (scheme == null) {
      scheme = ssl() == null ? "http" : "https";
    }
    return scheme;
  }

  private SslHandler ssl() {
    return (SslHandler)
        Stream.of(ctx.channel(), ctx.channel().parent())
            .filter(Objects::nonNull)
            .map(Channel::pipeline)
            .map(it -> it.get("ssl"))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
  }

  @Override
  public Context setScheme(String scheme) {
    this.scheme = scheme;
    return this;
  }

  @Override
  public int getPort() {
    return port > 0 ? port : DefaultContext.super.getPort();
  }

  @Override
  public Context setPort(int port) {
    this.port = port;
    return this;
  }

  @Override
  public Value header() {
    if (headers == null) {
      Map<String, Collection<String>> headerMap = new LinkedHashMap<>();
      HttpHeaders headers = req.headers();
      Set<String> names = headers.names();
      for (String name : names) {
        headerMap.put(name, headers.getAll(name));
      }
      this.headers = Value.headers(getValueFactory(), headerMap);
    }
    return headers;
  }

  @Override
  public Body body() {
    if (decoder != null && decoder.hasNext()) {
      return new NettyBody(this, (HttpData) decoder.next(), HttpUtil.getContentLength(req, -1L));
    }
    return Body.empty(this);
  }

  @Override
  public Map<String, String> cookieMap() {
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

  @Override
  public Context onComplete(Route.Complete task) {
    if (listeners == null) {
      listeners = new CompletionListeners();
    }
    listeners.addListener(task);
    return this;
  }

  //  @Override
  //  public Context upgrade(WebSocket.Initializer handler) {
  //    try {
  //      responseStarted = true;
  //      Config conf = getRouter().getConfig();
  //      int maxSize =
  //          conf.hasPath("websocket.maxSize")
  //              ? conf.getBytes("websocket.maxSize").intValue()
  //              : WebSocket.MAX_BUFFER_SIZE;
  //      String webSocketURL = getProtocol() + "://" + req.headers().get(HttpHeaderNames.HOST) +
  // path;
  //      var config =
  //          WebSocketDecoderConfig.newBuilder()
  //              .allowExtensions(true)
  //              .allowMaskMismatch(false)
  //              .withUTF8Validator(false)
  //              .maxFramePayloadLength(maxSize)
  //              .build();
  //      webSocket = new NettyWebSocket(this);
  //      handler.init(Context.readOnly(this), webSocket);
  //      var webSocketRequest =
  //          new DefaultFullHttpRequest(
  //              HTTP_1_1,
  //              req.method(),
  //              req.uri(),
  //              Unpooled.EMPTY_BUFFER,
  //              req.headers(),
  //              EmptyHttpHeaders.INSTANCE);
  //      var codec = ctx.pipeline().get(NettyServerCodec.class);
  //      codec.webSocketHandshake(ctx);
  //      WebSocketServerHandshakerFactory factory =
  //          new WebSocketServerHandshakerFactory(webSocketURL, null, config);
  //      WebSocketServerHandshaker handshaker = factory.newHandshaker(webSocketRequest);
  //      handshaker.handshake(ctx.channel(), webSocketRequest);
  //      webSocket.fireConnect();
  //      long timeout =
  //          conf.hasPath("websocket.idleTimeout")
  //              ? conf.getDuration("websocket.idleTimeout", MILLISECONDS)
  //              : MINUTES.toMillis(5);
  //      if (timeout > 0) {
  //        IdleStateHandler idle = new IdleStateHandler(timeout, 0, 0, MILLISECONDS);
  //        ctx.pipeline().addBefore("handler", "idle", idle);
  //      }
  //    } catch (Throwable x) {
  //      sendError(x);
  //    }
  //    return this;
  //  }
  @Override
  public Context upgrade(WebSocket.Initializer handler) {
    try {
      responseStarted = true;
      var conf = getRouter().getConfig();
      int maxSize =
          conf.hasPath("websocket.maxSize")
              ? conf.getBytes("websocket.maxSize").intValue()
              : WebSocket.MAX_BUFFER_SIZE;
      var webSocketURL = getProtocol() + "://" + req.headers().get(HttpHeaderNames.HOST) + path;

      var config =
          WebSocketDecoderConfig.newBuilder()
              .allowExtensions(true)
              .allowMaskMismatch(false)
              .withUTF8Validator(false)
              .maxFramePayloadLength(maxSize)
              .build();

      webSocket = new NettyWebSocket(this);
      handler.init(Context.readOnly(this), webSocket);

      var webSocketRequest =
          new DefaultFullHttpRequest(
              HTTP_1_1,
              req.method(),
              req.uri(),
              Unpooled.EMPTY_BUFFER,
              req.headers(),
              EmptyHttpHeaders.INSTANCE);

      var codec = ctx.pipeline().get(NettyServerCodec.class);
      codec.webSocketHandshake(ctx);

      var factory = new WebSocketServerHandshakerFactory(webSocketURL, null, config);
      var handshaker = factory.newHandshaker(webSocketRequest);

      if (handshaker == null) {
        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        return this;
      }

      handshaker.handshake(ctx.channel(), webSocketRequest);
      webSocket.fireConnect();

      long timeout =
          conf.hasPath("websocket.idleTimeout")
              ? conf.getDuration("websocket.idleTimeout", MILLISECONDS)
              : MINUTES.toMillis(5);

      if (timeout > 0) {
        IdleStateHandler idle = new IdleStateHandler(timeout, 0, 0, MILLISECONDS);
        // If the global server timeout is already there, replace it. Otherwise, add it.
        if (ctx.pipeline().get(IdleStateHandler.class) != null) {
          ctx.pipeline().replace(IdleStateHandler.class, "idle", idle);
        } else {
          ctx.pipeline().addBefore("handler", "idle", idle);
        }
      }

    } catch (Throwable x) {
      sendError(x);
    }
    return this;
  }

  @Override
  public Context upgrade(ServerSentEmitter.Handler handler) {
    responseStarted = true;
    ctx.writeAndFlush(new DefaultHttpResponse(HTTP_1_1, status, setHeaders));

    try {
      handler.handle(new NettyServerSentEmitter(this));
    } catch (Throwable x) {
      sendError(x);
    }
    return this;
  }

  /* **********************************************************************************************
   * Response methods:
   * **********************************************************************************************
   */

  @Override
  public StatusCode getResponseCode() {
    return StatusCode.valueOf(this.status.code());
  }

  @Override
  public Context setResponseCode(int statusCode) {
    this.status = HttpResponseStatus.valueOf(statusCode);
    return this;
  }

  @Override
  public Context setResponseHeader(String name, String value) {
    setHeaders.set(name, value);
    return this;
  }

  @Override
  public Context removeResponseHeader(String name) {
    setHeaders.remove(name);
    return this;
  }

  @Override
  public Context removeResponseHeaders() {
    setHeaders.clear();
    ifStreamId(this.streamId);
    return this;
  }

  @Override
  public MediaType getResponseType() {
    return responseType == null ? MediaType.text : responseType;
  }

  @Override
  public Context setDefaultResponseType(MediaType contentType) {
    if (responseType == null) {
      setResponseType(contentType);
    }
    return this;
  }

  @Override
  public final Context setResponseType(MediaType contentType) {
    this.responseType = contentType;
    setHeaders.set(CONTENT_TYPE, NettyString.valueOf(contentType));
    return this;
  }

  @Override
  public Context setResponseType(String contentType) {
    this.setResponseType(MediaType.valueOf(contentType));
    return this;
  }

  @Nullable @Override
  public String getResponseHeader(String name) {
    return setHeaders.get(name);
  }

  @Override
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

  public Context setResponseCookie(Cookie cookie) {
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

  @Override
  public PrintWriter responseWriter(MediaType type) {
    setResponseType(type);

    return new PrintWriter(
        new NettyWriter(newOutputStream(), ofNullable(type.getCharset()).orElse(UTF_8)));
  }

  @Override
  public Sender responseSender() {
    prepareChunked();
    ctx.write(new DefaultHttpResponse(HTTP_1_1, status, setHeaders));
    return new NettySender(this);
  }

  @Override
  public OutputStream responseStream() {
    return newOutputStream();
  }

  @Override
  public Context send(String data) {
    return send(data, UTF_8);
  }

  @Override
  public final Context send(String data, Charset charset) {
    return send(wrappedBuffer(data.getBytes(charset)));
  }

  @Override
  public final Context send(byte[] data) {
    return send(wrappedBuffer(data));
  }

  @Override
  public Context send(byte[]... data) {
    return send(Unpooled.wrappedBuffer(data));
  }

  @Override
  public Context send(ByteBuffer[] data) {
    return send(Unpooled.wrappedBuffer(data));
  }

  @Override
  public final Context send(ByteBuffer data) {
    return send(wrappedBuffer(data));
  }

  @Override
  public Context send(Output output) {
    output.send(this);
    return this;
  }

  private Context send(ByteBuf data) {
    return send(data, Integer.toString(data.readableBytes()));
  }

  Context send(ByteBuf data, CharSequence contentLength) {
    try {
      responseStarted = true;
      setHeaders.set(CONTENT_LENGTH, contentLength);
      var response = new DefaultFullHttpResponse(HTTP_1_1, status, data, setHeaders, NO_TRAILING);
      connection.writeMessage(response, promise());
      return this;
    } finally {
      requestComplete();
    }
  }

  @Override
  public Context send(ReadableByteChannel channel) {
    try {
      prepareChunked();
      int bufferSize = contentLength > 0 ? (int) contentLength : this.bufferSize;
      connection.writeChunks(
          new DefaultHttpResponse(HTTP_1_1, status, setHeaders),
          new ChunkedNioStream(channel, bufferSize),
          EMPTY_LAST_CONTENT,
          promise());
      return this;
    } finally {
      requestComplete();
    }
  }

  @Override
  public Context send(FileDownload file) {
    if (file.deleteOnComplete()) {
      register(
          new DeleteFileTask(
              router.getLog(), file.getFile().toString(), () -> Files.delete(file.getFile())));
    }
    return DefaultContext.super.send(file);
  }

  @Override
  public Context send(InputStream in) {
    if (in instanceof FileInputStream) {
      // use channel
      return send(((FileInputStream) in).getChannel());
    }
    try {
      long len = responseLength();
      ByteRange range = ByteRange.parse(req.headers().get(RANGE), len).apply(this);
      prepareChunked();
      connection.writeChunks(
          new DefaultHttpResponse(HTTP_1_1, status, setHeaders),
          new ChunkedStream(range.apply(in), bufferSize),
          EMPTY_LAST_CONTENT,
          promise());
      responseStarted = true;
      return this;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    } finally {
      requestComplete();
    }
  }

  @Override
  public Context send(FileChannel file) {
    try {
      long len = file.size();
      setHeaders.set(CONTENT_LENGTH, Long.toString(len));

      ByteRange range = ByteRange.parse(req.headers().get(RANGE), len).apply(this);

      var rsp = new DefaultHttpResponse(HTTP_1_1, status, setHeaders);
      responseStarted = true;

      if (preferChunked()) {
        prepareChunked();

        var chunkedInput =
            new HttpChunkedInput(
                new ChunkedNioFile(file, range.getStart(), range.getEnd(), bufferSize));

        connection.writeChunks(rsp, chunkedInput, promise());
      } else {
        connection.writeChunks(
            rsp,
            new DefaultFileRegion(file, range.getStart(), range.getEnd()),
            EMPTY_LAST_CONTENT,
            promise());
      }
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    } finally {
      requestComplete();
    }
    return this;
  }

  private boolean preferChunked() {
    // IOUring doesn't like File region
    return isSecure()
        || isGzip()
        || ctx.pipeline().channel().getClass().getName().startsWith("IOUring");
  }

  @Override
  public boolean isResponseStarted() {
    return responseStarted;
  }

  @Override
  public boolean getResetHeadersOnError() {
    return resetHeadersOnError == null
        ? getRouter().getRouterOptions().isResetHeadersOnError()
        : resetHeadersOnError;
  }

  @Override
  public Context setResetHeadersOnError(boolean value) {
    this.resetHeadersOnError = value;
    return this;
  }

  @Override
  public Context send(StatusCode statusCode) {
    try {
      setResponseCode(statusCode.value());
      responseStarted = true;
      if (contentLength == -1) {
        setHeaders.set(CONTENT_LENGTH, ZERO);
      }
      var rsp =
          new DefaultFullHttpResponse(
              HTTP_1_1, status, Unpooled.EMPTY_BUFFER, setHeaders, NO_TRAILING);
      connection.writeMessage(rsp, promise());
      return this;
    } finally {
      requestComplete();
    }
  }

  void requestComplete() {
    fireCompleteEvent();
    ifSaveSession();
  }

  private void fireCompleteEvent() {
    if (listeners != null) {
      listeners.run(this);
    }
  }

  private void ifSaveSession() {
    var session = getSession();
    if (session != null) {
      var store = router.getSessionStore();
      store.saveSession(this, session);
    }
  }

  private Session getSession() {
    return attributes == null ? null : (Session) attributes.get(Session.NAME);
  }

  public ChannelPromise promise() {
    return responsePromise == null ? ctx.voidPromise() : responsePromise;
  }

  public void setDecoder(InterfaceHttpPostRequestDecoder decoder) {
    this.decoder = decoder;
    responsePromise = getOrCreateResponsePromise();
    responsePromise.addListener(new DestroyDecoder(router.getLog(), decoder));
  }

  void log(Throwable cause) {
    if (Server.connectionLost(cause)) {
      router
          .getLog()
          .debug(
              "exception found while sending response {} {}", getMethod(), getRequestPath(), cause);
    } else {
      router
          .getLog()
          .error(
              "exception found while sending response {} {}", getMethod(), getRequestPath(), cause);
    }
  }

  private NettyOutputStream newOutputStream() {
    prepareChunked();
    return new NettyOutputStream(
        this, ctx, bufferSize, new DefaultHttpResponse(HTTP_1_1, status, setHeaders));
  }

  private void register(DeleteFileTask deleteFileTask) {
    responsePromise = getOrCreateResponsePromise();
    responsePromise.addListener(deleteFileTask);
  }

  private ChannelPromise getOrCreateResponsePromise() {
    if (responsePromise == null) {
      responsePromise = ctx.newPromise();
    }
    return responsePromise;
  }

  private void decodeForm(Formdata form) {
    if (decoder == null || decoder instanceof HttpRawPostRequestDecoder) {
      // empty/bad form
      return;
    }
    try {
      while (decoder.hasNext()) {
        HttpData next = (HttpData) decoder.next();
        if (next.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
          var fileUpoad =
              new NettyFileUpload(
                  router.getTmpdir(), (io.netty.handler.codec.http.multipart.FileUpload) next);
          register(new DeleteFileTask(router.getLog(), fileUpoad.getFileName(), fileUpoad::close));
          form.put(next.getName(), fileUpoad);
        } else {
          form.put(next.getName(), next.getString(UTF_8));
        }
      }
    } catch (HttpPostRequestDecoder.EndOfDataDecoderException x) {
      // ignore, silly netty
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  private long responseLength() {
    String len = setHeaders.get(CONTENT_LENGTH);
    return len == null ? -1 : Long.parseLong(len);
  }

  private void prepareChunked() {
    responseStarted = true;
    // remove flusher, doesn't play well with streaming/chunked responses
    var pipeline = ctx.pipeline();
    if (pipeline.get("chunker") == null) {
      pipeline.addBefore("handler", "chunker", new ChunkedWriteHandler());
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
    return require(ServerOptions.class).getCompressionLevel() != null;
  }
}

/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Server;
import io.jooby.ServerSentMessage;
import io.jooby.SneakyThrows;
import io.jooby.WebSocketCloseStatus;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import okio.ByteString;

public class WebClient implements AutoCloseable {

  private class SyncWebSocketListener extends WebSocketListener {

    private CountDownLatch opened = new CountDownLatch(1);

    private AtomicBoolean closed = new AtomicBoolean(false);

    private BlockingQueue messages = new LinkedBlockingQueue();

    private String testName;

    public SyncWebSocketListener(String testName) {
      this.testName = testName;
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
      opened.countDown();
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
      closed.set(true);
    }

    @Override
    public void onFailure(
        @NotNull WebSocket webSocket, @NotNull Throwable e, @Nullable Response response) {
      if (!Server.connectionLost(e)) {
        System.err.println("Unexpected web socket error: " + testName);
        e.printStackTrace();
      }
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
      messages.offer(text);
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
      messages.offer(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
    }

    public String lastMessage() {
      try {
        return (String) messages.poll(10, TimeUnit.SECONDS);
      } catch (Exception x) {
        throw SneakyThrows.propagate(x);
      }
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
      super.onClosing(webSocket, code, reason);
    }
  }

  public class BlockingWebSocket {
    private WebSocket ws;

    private SyncWebSocketListener listener;

    public BlockingWebSocket(WebSocket ws, SyncWebSocketListener listener) {
      this.ws = ws;
      this.listener = listener;
      try {
        this.listener.opened.await(5, TimeUnit.SECONDS);
      } catch (Exception x) {
        throw SneakyThrows.propagate(x);
      }
    }

    public String send(String message) {
      ws.send(message);
      return lastMessage();
    }

    public String sendBytes(byte[] message) {
      ws.send(ByteString.of(message));
      return lastMessage();
    }

    public String lastMessage() {
      return listener.lastMessage();
    }

    public void close() {
      if (listener.closed.compareAndSet(false, true)) {
        ws.close(WebSocketCloseStatus.NORMAL_CODE, WebSocketCloseStatus.NORMAL.getReason());
      }
    }
  }

  public class Request {
    private final okhttp3.Request.Builder req;
    private SneakyThrows.Consumer<okhttp3.Request.Builder> configurer;

    public Request(okhttp3.Request.Builder req) {
      this.req = req;
    }

    public Request prepare(SneakyThrows.Consumer<okhttp3.Request.Builder> configurer) {
      this.configurer = configurer;
      return this;
    }

    public void execute(SneakyThrows.Consumer<Response> callback) {
      execute(1, callback);
    }

    public void execute(int concurrency, SneakyThrows.Consumer<Response> callback) {
      if (configurer != null) {
        configurer.accept(req);
      }
      if (concurrency > 1) {
        var futures = new ArrayList<CompletableFuture<String>>();
        for (var i = 0; i < concurrency; i++) {
          futures.add(
              CompletableFuture.supplyAsync(
                  () -> {
                    executeCall(callback);
                    return "success";
                  }));
          try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
          } catch (CompletionException x) {
            throw SneakyThrows.propagate(x.getCause());
          }
        }
      } else {
        executeCall(callback);
      }
    }

    private void executeCall(SneakyThrows.Consumer<Response> callback) {
      okhttp3.Request r = req.build();
      try (Response rsp = client.newCall(r).execute()) {
        callback.accept(rsp);
      } catch (SocketTimeoutException x) {
        SocketTimeoutException timeout = new SocketTimeoutException(r.toString());
        timeout.addSuppressed(x);
        throw SneakyThrows.propagate(timeout);
      } catch (IOException x) {
        throw SneakyThrows.propagate(x);
      }
    }
  }

  private static RequestBody EMPTY_BODY = RequestBody.create(new byte[0], null);
  private String scheme;
  private final int port;
  private OkHttpClient client;
  private Headers.Builder headers;

  public WebClient(String scheme, int port, boolean followRedirects) {
    try {
      this.scheme = scheme;
      this.port = port;
      OkHttpClient.Builder builder =
          new OkHttpClient.Builder()
              .connectTimeout(5, TimeUnit.MINUTES)
              .writeTimeout(5, TimeUnit.MINUTES)
              .readTimeout(5, TimeUnit.MINUTES)
              .followRedirects(followRedirects);
      if (scheme.equalsIgnoreCase("https")) {
        configureSelfSigned(builder);
      }
      this.client = builder.build();
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  public WebClient header(String name, String value) {
    if (headers == null) {
      headers = new Headers.Builder();
    }
    if (value != null && !value.trim().isEmpty()) {
      headers.add(name, value);
    }
    return this;
  }

  public Request invoke(String method, String path) {
    return invoke(method, path, EMPTY_BODY);
  }

  public Request invoke(String method, String path, RequestBody body) {
    okhttp3.Request.Builder req = new okhttp3.Request.Builder();
    req.method(method, body);
    setRequestHeaders(req);
    req.url(scheme + "://localhost:" + port + path);
    return new Request(req);
  }

  private void setRequestHeaders(okhttp3.Request.Builder req) {
    if (headers == null) {
      // set default headers:
      header(
          "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
    }
    if (headers != null) {
      req.headers(headers.build());
      headers = null;
    }
  }

  public Request get(String path) {
    return invoke("GET", path, null);
  }

  public ServerSentMessageIterator sse(String path) {
    okhttp3.Request.Builder req = new okhttp3.Request.Builder();
    setRequestHeaders(req);
    req.url(scheme + "://localhost:" + port + path);
    EventSource.Factory factory = EventSources.createFactory(client);
    BlockingQueue<ServerSentMessage> messages = new LinkedBlockingQueue();
    EventSource eventSource =
        factory.newEventSource(
            req.build(),
            new EventSourceListener() {
              @Override
              public void onClosed(@NotNull EventSource eventSource) {
                eventSource.cancel();
              }

              @Override
              public void onEvent(
                  @NotNull EventSource eventSource,
                  @Nullable String id,
                  @Nullable String type,
                  @NotNull String data) {
                // retry is not part of public API
                ServerSentMessage message = new ServerSentMessage(data).setId(id).setEvent(type);
                messages.offer(message);
              }

              @Override
              public void onFailure(
                  @NotNull EventSource eventSource,
                  @Nullable Throwable t,
                  @Nullable Response response) {
                super.onFailure(eventSource, t, response);
              }

              @Override
              public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                super.onOpen(eventSource, response);
              }
            });
    return new ServerSentMessageIterator(eventSource, messages);
  }

  public void get(String path, SneakyThrows.Consumer<Response> callback) {
    get(path).execute(callback);
  }

  public void syncWebSocket(String path, SneakyThrows.Consumer<BlockingWebSocket> consumer) {
    okhttp3.Request.Builder req = new okhttp3.Request.Builder();
    req.url("ws://localhost:" + port + path);
    setRequestHeaders(req);
    okhttp3.Request r = req.build();
    SyncWebSocketListener listener =
        new SyncWebSocketListener(
            System.getProperty("___app_name__")
                + "("
                + System.getProperty("___server_name__")
                + ")");
    WebSocket webSocket = client.newWebSocket(r, listener);
    BlockingWebSocket blockingWebSocket = new BlockingWebSocket(webSocket, listener);
    consumer.accept(blockingWebSocket);
    blockingWebSocket.close();
  }

  public Request options(String path) {
    return invoke("OPTIONS", path, null);
  }

  public void options(String path, SneakyThrows.Consumer<Response> callback) {
    options(path).execute(callback);
  }

  public Request trace(String path) {
    return invoke("TRACE", path, null);
  }

  public void trace(String path, SneakyThrows.Consumer<Response> callback) {
    trace(path).execute(callback);
  }

  public Request head(String path) {
    return invoke("HEAD", path, null);
  }

  public void head(String path, SneakyThrows.Consumer<Response> callback) {
    head(path).execute(callback);
  }

  public Request post(String path) {
    return post(path, EMPTY_BODY);
  }

  public void post(String path, SneakyThrows.Consumer<Response> callback) {
    post(path).execute(callback);
  }

  public Request post(String path, RequestBody body) {
    return invoke("POST", path, body);
  }

  public void post(String path, RequestBody form, SneakyThrows.Consumer<Response> callback) {
    post(path, form).execute(callback);
  }

  public Request put(String path) {
    return invoke("put", path, EMPTY_BODY);
  }

  public void put(String path, SneakyThrows.Consumer<Response> callback) {
    put(path).execute(callback);
  }

  public Request delete(String path) {
    return invoke("delete", path, EMPTY_BODY);
  }

  public void delete(String path, SneakyThrows.Consumer<Response> callback) {
    delete(path).execute(callback);
  }

  public Request patch(String path) {
    return invoke("patch", path, EMPTY_BODY);
  }

  public void patch(String path, SneakyThrows.Consumer<Response> callback) {
    patch(path).execute(callback);
  }

  public int getPort() {
    return port;
  }

  public void close() {
    client.dispatcher().executorService().shutdown();
    client.connectionPool().evictAll();
  }

  private static void configureSelfSigned(OkHttpClient.Builder builder)
      throws NoSuchAlgorithmException, KeyManagementException {
    X509TrustManager trustManager =
        new X509TrustManager() {
          @Override
          public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
          }

          @Override
          public void checkServerTrusted(final X509Certificate[] chain, final String authType)
              throws CertificateException {}

          @Override
          public void checkClientTrusted(final X509Certificate[] chain, final String authType)
              throws CertificateException {}
        };

    SSLContext sslContext = SSLContext.getInstance("SSL");

    sslContext.init(null, new TrustManager[] {trustManager}, new java.security.SecureRandom());
    builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
    builder.hostnameVerifier((hostname, session) -> true);
  }

  public static class ServerSentMessageIterator {
    private final EventSource source;
    private List<BiConsumer<ServerSentMessage, EventSource>> consumers = new ArrayList<>();

    private BlockingQueue<ServerSentMessage> messages;

    public ServerSentMessageIterator(
        EventSource source, BlockingQueue<ServerSentMessage> messages) {
      this.source = source;
      this.messages = messages;
    }

    public ServerSentMessageIterator next(Consumer<ServerSentMessage> consumer) {
      return next((message, source) -> consumer.accept(message));
    }

    public ServerSentMessageIterator next(BiConsumer<ServerSentMessage, EventSource> consumer) {
      consumers.add(consumer);
      return this;
    }

    public void verify() {
      int i = 0;
      while (i < consumers.size()) {
        try {
          ServerSentMessage message = messages.take();
          consumers.get(i).accept(message, source);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        i += 1;
      }
    }
  }
}

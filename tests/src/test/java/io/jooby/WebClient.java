package io.jooby;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class WebClient implements AutoCloseable {

  private class SyncWebSocketListener extends WebSocketListener {

    private CountDownLatch opened = new CountDownLatch(1);

    private CountDownLatch closed = new CountDownLatch(1);

    private List<Throwable> errors = new ArrayList<>();

    private BlockingQueue messages = new LinkedBlockingQueue();

    @Override public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
      opened.countDown();
    }

    @Override public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
      closed.countDown();
    }

    @Override public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable e,
        @Nullable Response response) {
      errors.add(e);
    }

    @Override public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
      messages.offer(text);
    }

    public String lastMessage() {
      try {
        return (String) messages.poll(5, TimeUnit.SECONDS);
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
      return listener.lastMessage();
    }
  }

  public class Request {
    private final okhttp3.Request.Builder req;

    public Request(okhttp3.Request.Builder req) {
      this.req = req;
    }

    public Request prepare(SneakyThrows.Consumer<okhttp3.Request.Builder> configurer) {
      configurer.accept(req);
      return this;
    }

    public void execute(SneakyThrows.Consumer<Response> callback) {
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
  private final int port;
  private OkHttpClient client;
  private Map<String, String> headers;

  public WebClient(int port, boolean followRedirects) {
    this.port = port;
    client = new OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .followRedirects(followRedirects)
        .build();
    header("Accept",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
  }

  public WebClient header(String name, String value) {
    if (headers == null) {
      headers = new HashMap<>();
    }
    headers.put(name, value);
    return this;
  }

  public Request invoke(String method, String path) {
    return invoke(method, path, EMPTY_BODY);
  }

  public Request invoke(String method, String path, RequestBody body) {
    okhttp3.Request.Builder req = new okhttp3.Request.Builder();
    req.method(method, body);
    setRequestHeaders(req);
    req.url("http://localhost:" + port + path);
    return new Request(req);
  }

  private void setRequestHeaders(okhttp3.Request.Builder req) {
    if (headers != null) {
      req.headers(Headers.of(headers));
      headers = null;
      header("Accept",
          "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
    }
  }

  public Request get(String path) {
    return invoke("GET", path, null);
  }

  public void get(String path, SneakyThrows.Consumer<Response> callback) {
    get(path).execute(callback);
  }

  public WebSocket webSocket(String path, WebSocketListener listener) {
    okhttp3.Request.Builder req = new okhttp3.Request.Builder();
    req.url("ws://localhost:" + port + path);
    setRequestHeaders(req);
    okhttp3.Request r = req.build();
    return client.newWebSocket(r, listener);
  }

  public WebSocket syncWebSocket(String path, SneakyThrows.Consumer<BlockingWebSocket> consumer) {
    okhttp3.Request.Builder req = new okhttp3.Request.Builder();
    req.url("ws://localhost:" + port + path);
    setRequestHeaders(req);
    okhttp3.Request r = req.build();
    SyncWebSocketListener listener = new SyncWebSocketListener();
    WebSocket webSocket = client.newWebSocket(r, listener);
    consumer.accept(new BlockingWebSocket(webSocket, listener));
    return webSocket;
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
}

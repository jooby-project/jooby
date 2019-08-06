package io.jooby;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WebClient {

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
    header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
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
    if (headers != null) {
      req.headers(Headers.of(headers));
      headers = null;
      header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
    }
    req.url("http://localhost:" + port + path);
    return new Request(req);
  }

  public Request get(String path) {
    return invoke("GET", path, null);
  }

  public void get(String path, SneakyThrows.Consumer<Response> callback) {
    get(path).execute(callback);
  }

  public Request options(String path) {
    return invoke("OPTIONS", path, null);
  }

  public void options(String path, SneakyThrows.Consumer<Response> callback) {
    options(path).execute(callback);
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
}

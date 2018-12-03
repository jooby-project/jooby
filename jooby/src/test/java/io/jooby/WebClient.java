package io.jooby;

import okhttp3.*;
import io.jooby.Throwing;

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

    public Request prepare(Throwing.Consumer<okhttp3.Request.Builder> configurer) {
      configurer.accept(req);
      return this;
    }

    public void execute(Throwing.Consumer<Response> callback) {
      okhttp3.Request r = req.build();
      try (Response rsp = client.newCall(r).execute()) {
        callback.accept(rsp);
      } catch (SocketTimeoutException x) {
        SocketTimeoutException timeout = new SocketTimeoutException(r.toString());
        timeout.addSuppressed(x);
        throw Throwing.sneakyThrow(timeout);
      } catch (IOException x) {
        throw Throwing.sneakyThrow(x);
      }
    }
  }

  private static RequestBody EMPTY_BODY = RequestBody.create(null, new byte[0]);
  private final int port;
  private OkHttpClient client;
  private Map<String, String> headers;

  public WebClient(int port) {
    this.port = port;
    client = new OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .build();
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
    }
    req.url("http://localhost:" + port + path);
    return new Request(req);
  }

  public Request get(String path) {
    return invoke("GET", path, null);
  }

  public void get(String path, Throwing.Consumer<Response> callback) {
    get(path).execute(callback);
  }

  public Request post(String path) {
    return post(path, EMPTY_BODY);
  }

  public void post(String path, Throwing.Consumer<Response> callback) {
    post(path).execute(callback);
  }

  public Request post(String path, RequestBody form) {
    return invoke("POST", path, form);
  }

  public void post(String path, RequestBody form, Throwing.Consumer<Response> callback) {
    post(path, form).execute(callback);
  }

  public Request put(String path) {
    return invoke("put", path, EMPTY_BODY);
  }

  public void put(String path, Throwing.Consumer<Response> callback) {
    put(path).execute(callback);
  }

  public Request delete(String path) {
    return invoke("delete", path, EMPTY_BODY);
  }

  public void delete(String path, Throwing.Consumer<Response> callback) {
    delete(path).execute(callback);
  }

  public Request patch(String path) {
    return invoke("patch", path, EMPTY_BODY);
  }

  public void patch(String path, Throwing.Consumer<Response> callback) {
    patch(path).execute(callback);
  }
}

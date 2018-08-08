package io.jooby.test;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jooby.funzy.Throwing;

import java.io.IOException;
import java.net.SocketTimeoutException;
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

  public WebClient(int port) {
    this.port = port;
    client = new OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .build();
  }

  public Request execute(String method, String path, RequestBody body) {
    okhttp3.Request.Builder req = new okhttp3.Request.Builder();
    req.method(method, body);
    req.url("http://localhost:" + port + path);
    return new Request(req);
  }

  public Request get(String path) {
    return execute("GET", path, null);
  }

  public void get(String path, Throwing.Consumer<Response> callback) {
    get(path).execute(callback);
  }

  public Request post(String path) {
    return execute("post", path, EMPTY_BODY);
  }

  public void post(String path, Throwing.Consumer<Response> callback) {
    post(path).execute(callback);
  }

  public Request put(String path) {
    return execute("put", path, EMPTY_BODY);
  }

  public void put(String path, Throwing.Consumer<Response> callback) {
    put(path).execute(callback);
  }

  public Request delete(String path) {
    return execute("delete", path, EMPTY_BODY);
  }

  public void delete(String path, Throwing.Consumer<Response> callback) {
    delete(path).execute(callback);
  }

  public Request patch(String path) {
    return execute("patch", path, EMPTY_BODY);
  }

  public void patch(String path, Throwing.Consumer<Response> callback) {
    patch(path).execute(callback);
  }
}

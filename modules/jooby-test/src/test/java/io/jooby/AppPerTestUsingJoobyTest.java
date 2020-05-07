package io.jooby;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AppPerTestUsingJoobyTest {

  static OkHttpClient client = new OkHttpClient();

  @JoobyTest(value = TestApp.class)
  public void sayHi(String serverPath) throws IOException {
    Request request = new Request.Builder()
        .url(serverPath)
        .build();

    try (Response response = client.newCall(request).execute()) {
      assertEquals("OK", response.body().string());
    }
  }

  @JoobyTest(value = TestApp.class)
  public void sayH2i(int serverPort) throws IOException {
    Request request = new Request.Builder()
        .url("http://localhost:" + serverPort + "/test")
        .build();

    try (Response response = client.newCall(request).execute()) {
      assertEquals("OK", response.body().string());
    }
  }

  @JoobyTest(value = TestArgApp.class, factoryMethod = "createApp")
  public void shouldUseFactoryMethod(int serverPort) throws IOException {
    Request request = new Request.Builder()
        .url("http://localhost:" + serverPort + "/")
        .build();

    try (Response response = client.newCall(request).execute()) {
      assertEquals("TEST", response.body().string());
    }
  }

  @JoobyTest(value = TestArgApp.class, factoryMethod = "createStaticApp")
  public void shouldUseStaticFactoryMethod(int serverPort) throws IOException {
    Request request = new Request.Builder()
        .url("http://localhost:" + serverPort + "/")
        .build();

    try (Response response = client.newCall(request).execute()) {
      assertEquals("TEST", response.body().string());
    }
  }

  public Jooby createApp() {
    return new TestArgApp("TEST");
  }

  public static Jooby createStaticApp() {
    return new TestArgApp("TEST");
  }
}

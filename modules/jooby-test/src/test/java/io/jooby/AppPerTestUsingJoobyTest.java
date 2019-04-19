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
}

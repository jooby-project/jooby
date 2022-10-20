/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@JoobyTest(value = TestApp.class, port = 0)
public class SingleAppUsingJoobyTest {

  static OkHttpClient client = new OkHttpClient();

  public String serverPath;

  public int serverPort;

  @Test
  public void sayHi() throws IOException {
    Request request = new Request.Builder().url(serverPath).build();

    try (Response response = client.newCall(request).execute()) {
      assertEquals("OK", response.body().string());
    }
  }

  @Test
  public void sayH2i() throws IOException {
    Request request = new Request.Builder().url("http://localhost:" + serverPort + "/test").build();

    try (Response response = client.newCall(request).execute()) {
      assertEquals("OK", response.body().string());
    }
  }
}

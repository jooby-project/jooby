/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.jooby.SneakyThrows;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class WebjarTest {
  private static final String VUE = vueVersion();

  private static String vueVersion() {
    try (InputStream vueprops =
        FeaturedTest.class
            .getClassLoader()
            .getResourceAsStream("META-INF/maven/org.webjars.npm/vue/pom.properties")) {
      Properties properties = new Properties();
      properties.load(vueprops);
      return properties.getProperty("version");
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  @ServerTest
  public void webjar(ServerTestRunner runner) throws IOException {
    String vueSize =
        String.valueOf(
            getClass()
                .getResource("/META-INF/resources/webjars/vue/" + VUE + "/dist/vue.cjs.js")
                .openConnection()
                .getContentLength());
    runner
        .define(
            app -> {
              app.assets("/jar/*", "/META-INF/resources/webjars/vue/" + VUE);
              app.assets("/jar2/*", "/META-INF/resources/webjars/vue/" + VUE + "/dist");
            })
        .ready(
            client -> {
              // Inside jar
              client.get(
                  "/jar/dist/vue.cjs.js",
                  rsp -> {
                    assertEquals(
                        "application/javascript;charset=utf-8",
                        rsp.header("Content-Type").toLowerCase());
                    assertEquals(vueSize, rsp.header("Content-Length").toLowerCase());
                  });
              client.get(
                  "/jar2/dist/../package.json",
                  rsp -> {
                    assertEquals(404, rsp.code());
                  });
              client.get(
                  "/jar/dist/nope.js",
                  rsp -> {
                    assertEquals(404, rsp.code());
                  });
              client.get(
                  "/jar/dist",
                  rsp -> {
                    assertEquals(404, rsp.code());
                  });
            });
  }
}

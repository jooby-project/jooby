/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3267;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Function;

import io.jooby.MediaType;
import io.jooby.handler.Asset;
import io.jooby.handler.AssetHandler;
import io.jooby.handler.AssetSource;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public class Issue3267 {

  @ServerTest
  public void shouldSupportCustomCharset(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              var source = AssetSource.create(app.getClassLoader(), "/www");
              app.assets("/3267/default/?*", new AssetHandler(source));

              // GBK
              var gbk = MediaType.valueOf("text/html;charset=GBK");
              Function<Asset, MediaType> overrideCharset =
                  asset -> {
                    var defaultType = asset.getContentType();
                    // Choose what is best for you
                    if (defaultType.matches(gbk)) {
                      return gbk;
                    }
                    return defaultType;
                  };
              app.assets(
                  "/3267/gbk/?*", new AssetHandler(source).setMediaTypeResolver(overrideCharset));
            })
        .ready(
            http -> {
              http.get(
                  "/3267/default",
                  rsp -> {
                    assertEquals("index.html", rsp.body().string().trim());
                    assertEquals(
                        "text/html;charset=utf-8",
                        rsp.body().contentType().toString().toLowerCase());
                  });
              http.get(
                  "/3267/gbk",
                  rsp -> {
                    assertEquals("index.html", rsp.body().string().trim());
                    assertEquals(
                        "text/html;charset=gbk", rsp.body().contentType().toString().toLowerCase());
                  });
            });
  }
}

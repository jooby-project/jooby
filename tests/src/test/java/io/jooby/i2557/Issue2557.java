/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2557;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.value.ConversionHint;
import okhttp3.FormBody;

public class Issue2557 {
  public record MyStrict(UUID uuid) {}

  public record MyFlexible(UUID uuid) {}

  public record AllGoodWithPartialMatching(UUID uuid, String name) {}

  @ServerTest
  public void shouldWorkWithEmptyUUID(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());

              app.post(
                  "/2557/strict",
                  ctx -> {
                    return ctx.form(MyStrict.class);
                  });
              app.post(
                  "/2557/flexible",
                  ctx -> {
                    return ctx.getValueFactory()
                        .convert(MyFlexible.class, ctx.form(), ConversionHint.Empty);
                  });
              app.post(
                  "/2557/partial",
                  ctx -> {
                    return ctx.form(AllGoodWithPartialMatching.class);
                  });
            })
        .ready(
            http -> {
              http.post(
                  "/2557/strict",
                  new FormBody.Builder().add("uuid", "").build(),
                  rsp -> {
                    var body = rsp.body().string();
                    assertTrue(
                        body.contains(
                            "Cannot convert value: &apos;null&apos;, to:"
                                + " &apos;io.jooby.i2557.Issue2557$MyStrict&apos;"),
                        body);
                  });
              http.post(
                  "/2557/flexible",
                  new FormBody.Builder().add("uuid", "").build(),
                  rsp -> {
                    assertEquals("{\"uuid\":null}", rsp.body().string());
                  });
              http.post(
                  "/2557/partial",
                  new FormBody.Builder().add("uuid", "").add("name", "some").build(),
                  rsp -> {
                    assertEquals("{\"uuid\":null,\"name\":\"some\"}", rsp.body().string());
                  });
            });
  }
}

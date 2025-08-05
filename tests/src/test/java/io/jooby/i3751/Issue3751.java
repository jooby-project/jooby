/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3751;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import io.jooby.hibernate.validator.HibernateValidatorModule;
import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.validation.BeanValidator;
import jakarta.validation.Validator;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class Issue3751 {
  @ServerTest
  public void shouldValidateDataBody(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new HibernateValidatorModule());
              app.install(new JacksonModule());
              app.post(
                  "/3751-direct",
                  (ctx -> {
                    Validator validator = ctx.require(Validator.class);
                    return validator.validate(ctx.body(User3721.class));
                  }));
              app.post(
                  "/3751-bean-validator",
                  BeanValidator.validate(
                      ctx -> {
                        ctx.body(User3721.class);
                        // all good
                        return List.of();
                      }));
            })
        .ready(
            http -> {
              var user =
                  RequestBody.create(
                      "{\"password\": \"1234\"}", MediaType.parse("application/json"));
              http.post(
                  "/3751-direct",
                  user,
                  rsp -> {
                    http.post(
                        "/3751-direct",
                        user,
                        rsp2 -> {
                          assertEquals(rsp.body().string(), rsp2.body().string());
                        });
                  });
            });
  }
}

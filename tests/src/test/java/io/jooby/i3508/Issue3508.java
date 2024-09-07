/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3508;

import static io.jooby.validation.BeanValidator.validate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.StatusCode;
import io.jooby.hibernate.validator.HibernateValidatorModule;
import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.validation.BeanValidator;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class Issue3508 {
  @ServerTest
  public void shouldValidateUsingProxy(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());
              app.install(new HibernateValidatorModule());

              app.get("/3508/query", validate(ctx -> ctx.query().to(Bean3508.class)));

              app.post("/3508/post", validate(ctx -> ctx.body(Bean3508.class)));

              app.use(BeanValidator.validate());
              app.get("/3508/queryWithType", ctx -> ctx.query(Bean3508.class));
            })
        .ready(
            http -> {
              http.get(
                  "/3508/queryWithType",
                  rsp -> {
                    assertEquals(StatusCode.UNPROCESSABLE_ENTITY.value(), rsp.code());
                  });
              http.get(
                  "/3508/query",
                  rsp -> {
                    assertEquals(StatusCode.UNPROCESSABLE_ENTITY.value(), rsp.code());
                  });
              http.post(
                  "/3508/post",
                  RequestBody.create("{}", MediaType.parse("application/json")),
                  rsp -> {
                    assertEquals(StatusCode.UNPROCESSABLE_ENTITY.value(), rsp.code());
                  });
            });
  }
}

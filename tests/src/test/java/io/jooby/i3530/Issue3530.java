/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3530;

import static org.junit.jupiter.api.Assertions.*;

import io.jooby.StatusCode;
import io.jooby.hibernate.validator.HibernateValidatorModule;
import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.validation.BeanValidator;
import io.jooby.validation.ValidationContext;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class Issue3530 {

  @ServerTest
  public void shouldRunValidationOnce(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());
              app.install(new HibernateValidatorModule());

              app.use(BeanValidator.validate());

              app.post(
                  "/3530/lambda",
                  ctx -> {
                    assertInstanceOf(ValidationContext.class, ctx);
                    return ctx.body(Bean3530.class);
                  });

              // Must NOT be validation context:
              app.use(
                  next ->
                      ctx -> {
                        assertFalse(ctx instanceof ValidationContext);
                        return next.apply(ctx);
                      });
              app.mvc(new Controller3530_());
            })
        .ready(
            http -> {
              http.post(
                  "/3530/lambda",
                  RequestBody.create("{}", MediaType.parse("application/json")),
                  rsp -> {
                    assertEquals(StatusCode.UNPROCESSABLE_ENTITY.value(), rsp.code());
                  });
              http.post(
                  "/3530/controller",
                  RequestBody.create("{}", MediaType.parse("application/json")),
                  rsp -> {
                    assertEquals(StatusCode.UNPROCESSABLE_ENTITY.value(), rsp.code());
                  });
            });
  }
}

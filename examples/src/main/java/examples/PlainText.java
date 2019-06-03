/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.annotations.Dispatch;
import io.jooby.annotations.GET;
import io.jooby.annotations.PathParam;

import java.util.Optional;

@Dispatch
public class PlainText {
  @GET("/plaintext")
  public String plainText(@PathParam Optional<String> message) {
    return message.orElse("Hello, World!");
  }

  @GET("/single")
  @Dispatch("single")
  public String single() {
    return Thread.currentThread().getName();
  }

  @GET("/")
  public String loop() {
    return Thread.currentThread().getName();
  }
}

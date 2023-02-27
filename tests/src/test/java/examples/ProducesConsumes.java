/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.annotation.GET;

public class ProducesConsumes {

  @GET(
      path = "/produces",
      produces = {"application/json", "application/xml"})
  public Message produces() {
    return new Message("MVC");
  }

  @GET(
      path = "/consumes",
      consumes = {"application/json", "application/xml"})
  public String consumes(Message body) {
    return body.toString();
  }
}

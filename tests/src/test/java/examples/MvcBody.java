/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import java.util.Map;
import java.util.Optional;

import io.jooby.annotation.POST;
import io.jooby.annotation.Path;
import io.jooby.annotation.QueryParam;

public class MvcBody {
  @POST
  @Path(("/body/str"))
  public String bodyString(String body) {
    return String.valueOf(body);
  }

  @POST
  @Path(("/body/int"))
  public int bodyInt(int body) {
    return body;
  }

  @POST
  @Path(("/body/json"))
  public Object bodyInt(Map<String, Object> body, @QueryParam Optional<String> type) {
    return body + type.orElse("null");
  }
}

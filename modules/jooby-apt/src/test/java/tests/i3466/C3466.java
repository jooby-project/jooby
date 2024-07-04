/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3466;

import java.util.Map;
import java.util.UUID;

import io.jooby.ParamSource;
import io.jooby.annotation.*;

@Path("/simple")
public class C3466 {

  @POST("/test")
  public String post(
      @Param(
              value = {ParamSource.QUERY, ParamSource.FORM},
              name = "client_id")
          UUID clientId,
      Map<String, Object> ignoredBodyName) {
    return "hello";
  }
}

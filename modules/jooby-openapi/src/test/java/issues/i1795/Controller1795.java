/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1795;

import java.util.List;

import io.jooby.annotation.POST;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

public class Controller1795 {
  @POST("/param")
  public List<String> create(
      @RequestBody(required = true, description = "String list") List<String> list) {
    return list;
  }

  @POST("/method")
  @RequestBody(description = "At method level list")
  public List<String> createAtMethod(List<String> list) {
    return list;
  }
}

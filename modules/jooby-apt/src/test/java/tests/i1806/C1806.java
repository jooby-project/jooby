/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i1806;

import java.util.List;

import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class C1806 {
  @GET("/1806/c")
  public List<String> sayHi(@QueryParam List<String> names) {
    return names;
  }
}

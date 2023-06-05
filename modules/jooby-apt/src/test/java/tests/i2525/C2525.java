/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2525;

import java.util.List;

import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class C2525 {
  @GET("/2525")
  public String something(@QueryParam List<Foo2525> foo) {
    return foo.toString();
  }

  @GET("/2325/bean")
  public String getMy(@QueryParam MyID2525 myId) {
    return myId.toString();
  }
}

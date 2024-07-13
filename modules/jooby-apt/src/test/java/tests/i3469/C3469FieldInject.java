/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3469;

import io.jooby.annotation.GET;
import jakarta.inject.Inject;

public class C3469FieldInject {

  @Inject private Foo3469 foo;

  @GET("/3469")
  public String method() {
    return foo.foo();
  }
}

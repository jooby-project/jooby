/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3469;

import java.util.List;

import io.jooby.annotation.GET;

public class C3469 {

  private Foo3469 foo;
  private List<Bar3469> bar;

  public C3469(Foo3469 foo, List<Bar3469> bar) {
    this.foo = foo;
    this.bar = bar;
  }

  public C3469(Foo3469 foo) {
    this.foo = foo;
    this.bar = List.of();
  }

  @GET("/3469")
  public String method() {
    return foo.foo() + bar.toString();
  }
}

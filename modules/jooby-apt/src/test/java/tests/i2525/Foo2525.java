/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i2525;

import java.util.Objects;

public class Foo2525 {
  public Integer a;
  public Integer b;

  public Foo2525(Integer a, Integer b) {
    Objects.requireNonNull(a);
    Objects.requireNonNull(b);

    this.a = a;
    this.b = b;
  }

  @Override
  public String toString() {
    return "{" + "a:" + a + ", b:" + b + '}';
  }
}

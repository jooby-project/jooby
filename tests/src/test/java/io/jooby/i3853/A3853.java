/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3853;

import io.avaje.jsonb.Json;

@Json
public class A3853 {
  private final String city;
  private final L3853 loc;

  public A3853(String city, L3853 loc) {
    this.city = city;
    this.loc = loc;
  }

  public String getCity() {
    return city;
  }

  public L3853 getLoc() {
    return loc;
  }
}

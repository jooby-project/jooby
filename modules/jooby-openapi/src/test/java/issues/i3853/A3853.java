/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3853;

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

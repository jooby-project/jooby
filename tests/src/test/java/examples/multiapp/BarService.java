/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.multiapp;

public class BarService {
  public BarService() {}

  public String hello(String name) {
    return "Bar: " + name;
  }
}

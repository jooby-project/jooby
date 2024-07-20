/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3476;

public class Box<T> {
  private T value;

  public Box(T value) {}

  public T getValue() {
    return value;
  }
}

/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

public class Message {
  private final String value;

  public Message(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }
}

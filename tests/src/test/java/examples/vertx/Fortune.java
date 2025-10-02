/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.vertx;

public final class Fortune implements Comparable<Fortune> {
  public final int id;

  public final String message;

  public Fortune(int id, String message) {
    this.id = id;
    this.message = message;
  }

  @Override
  public int compareTo(Fortune other) {
    return message.compareTo(other.message);
  }
}

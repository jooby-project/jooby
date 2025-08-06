/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input;

import javadoc.input.sub.Base;

/**
 * Subclass summary.
 *
 * <p>Subclass description.
 */
public class Subclass extends Base {
  private int number;

  /**
   * Number on subclass.
   *
   * @return Number on subclass.
   */
  public int getNumber() {
    return number;
  }

  /**
   * Name on subclass.
   *
   * @return Name on subclass.
   */
  @Override
  public String getName() {
    return super.getName();
  }
}

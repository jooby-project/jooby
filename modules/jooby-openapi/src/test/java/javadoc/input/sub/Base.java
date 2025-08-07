/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input.sub;

/**
 * Base summary.
 *
 * <p>Base description.
 */
public class Base {

  private String name;

  /** Desc on base class. */
  private String description;

  /**
   * Name on base class.
   *
   * @return On base class.
   */
  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }
}

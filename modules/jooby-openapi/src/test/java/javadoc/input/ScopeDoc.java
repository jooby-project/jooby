/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc.input;

// Ignored
/**
 * Class
 *
 * @version 3.40.1
 */
// Ignored
public class ScopeDoc {

  /** Nested */
  public static class Nested {
    /** Nested field. */
    private String nestedType;
  }

  // Ignored
  /** Field */
  // Ignored
  private String name;

  /**
   * Ignored.
   *
   * @param name
   */
  public ScopeDoc(String name) {
    this.name = name;
  }

  /**
   * Method
   *
   * @return Method.
   */
  public String getName() { // ignored
    /** ignored */
    return name;
    // yet ignored
  }
  // Still ignored.
}

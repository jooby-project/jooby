/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

/**
 * Fluent interface allowing to conveniently search context parameters in multiple sources.
 *
 * <pre>{@code
 * Value foo = ctx.lookup()
 *   .inQuery()
 *   .inPath()
 *   .get("foo");
 * }</pre>
 *
 * @see Context#lookup()
 * @see Context#lookup(String, ParamSource...)
 */
public interface ParamLookup {

  /**
   * Adds the specified source to the search locations.
   *
   * @param source The source to add.
   * @return This instance.
   */
  Stage in(ParamSource source);

  /**
   * Adds the path parameters to the search locations.
   *
   * @return This instance.
   */
  default Stage inPath() {
    return in(ParamSource.PATH);
  }

  /**
   * Adds the header parameters to the search locations.
   *
   * @return This instance.
   */
  default Stage inHeader() {
    return in(ParamSource.HEADER);
  }

  /**
   * Adds the cookie parameters to the search locations.
   *
   * @return This instance.
   */
  default Stage inCookie() {
    return in(ParamSource.COOKIE);
  }

  /**
   * Adds the flash parameters to the search locations.
   *
   * @return This instance.
   */
  default Stage inFlash() {
    return in(ParamSource.FLASH);
  }

  /**
   * Adds the session parameters to the search locations.
   *
   * @return This instance.
   */
  default Stage inSession() {
    return in(ParamSource.SESSION);
  }

  /**
   * Adds the query parameters to the search locations.
   *
   * @return This instance.
   */
  default Stage inQuery() {
    return in(ParamSource.QUERY);
  }

  /**
   * Adds the form parameters to the search locations.
   *
   * @return This instance.
   */
  default Stage inForm() {
    return in(ParamSource.FORM);
  }

  /**
   * Fluent interface allowing to conveniently search context parameters in multiple sources.
   *
   * <pre>{@code
   * Value foo = ctx.lookup()
   *   .inQuery()
   *   .inPath()
   *   .get("foo");
   * }</pre>
   *
   * @see Context#lookup()
   * @see Context#lookup(String, ParamSource...)
   */
  interface Stage extends ParamLookup {

    /**
     * Searches for a parameter in the specified sources, in the specified order, returning the
     * first non-missing {@link Value}, or a 'missing' {@link Value} if none found.
     *
     * @param name The name of the parameter.
     * @return The first non-missing {@link Value} or a {@link Value} representing a missing value
     *     if none found.
     */
    Value get(String name);
  }
}

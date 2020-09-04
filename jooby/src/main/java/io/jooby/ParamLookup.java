/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.Optional;

/**
 * Fluent interface allowing to conveniently search context parameters
 * in multiple sources.
 *
 * <pre>{@code
 *  Value foo = ctx.lookup()
 *    .inQuery()
 *    .inPath()
 *    .get("foo");
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
   * Adds the multipart parameters to the search locations.
   *
   * @return This instance.
   */
  default Stage inMultipart() {
    return in(ParamSource.MULTIPART);
  }

  /**
   * Fluent interface allowing to conveniently search context parameters
   * in multiple sources.
   *
   * <pre>{@code
   *  Value foo = ctx.lookup()
   *    .inQuery()
   *    .inPath()
   *    .get("foo");
   * }</pre>
   *
   * @see Context#lookup()
   * @see Context#lookup(String, ParamSource...)
   */
  interface Stage extends ParamLookup {

    /**
     * Searches for a parameter in the specified sources, in the specified
     * order, returning the first non-missing {@link Value}, or a 'missing'
     * {@link Value} if none found.
     *
     * @param name The name of the parameter.
     * @return The first non-missing {@link Value} or a {@link Value} representing
     * a missing value if none found.
     */
    Value get(String name);

    /**
     * Wraps the result of {@link #get(String)} in an {@link Optional} if the
     * value is a {@link ValueNode} or returns an empty {@link Optional}
     * otherwise.
     *
     * @param name The name of the parameter.
     * @return An {@link Optional} wrapping the result of {@link #get(String)}
     */
    default Optional<ValueNode> getNode(String name) {
      return Optional.of(get(name))
          .map(v -> v instanceof ValueNode ? (ValueNode) v : null);
    }
  }
}

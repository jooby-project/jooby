package io.jooby;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

/**
 * The SameSite attribute of the Set-Cookie HTTP response header allows you to declare
 * if your cookie should be restricted to a first-party or same-site context.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite">
 *   https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite</a>
 */
public enum SameSite {

  /**
   * Cookies are allowed to be sent with top-level navigations and will be sent along with
   * GET request initiated by third party website. This is the default value in modern browsers.
   */
  LAX("Lax"),

  /**
   * Cookies will only be sent in a first-party context and not be sent along with
   * requests initiated by third party websites.
   */
  STRICT("Strict"),

  /**
   * Cookies will be sent in all contexts, i.e sending cross-origin is allowed.
   * Requires the {@code Secure} attribute in latest browser versions.
   */
  NONE("None");

  SameSite(String value) {
    this.value = value;
  }

  private final String value;

  /**
   * Returns the parameter value used in {@code Set-Cookie}.
   *
   * @return the parameter value.
   */
  public String getValue() {
    return value;
  }

  /**
   * Returns whether this value requires the cookie to be flagged as {@code Secure}.
   *
   * @return {@code true} if the cookie should be secure.
   */
  public boolean requiresSecure() {
    return this == NONE;
  }

  /**
   * Returns an instance of this class based on value it uses in {@code Set-Cookie}.
   *
   * @param value the value.
   * @return an instance of this class.
   * @see #getValue()
   * @throws IllegalArgumentException if an invalid value is specified.
   */
  public static SameSite of(String value) {
    return stream(values())
        .filter(v -> v.getValue().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid SameSite value '"
            + value + "'. Use one of: " + stream(values())
            .map(SameSite::getValue)
            .collect(joining(", "))));
  }
}

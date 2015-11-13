/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.jooby.internal.CookieImpl;

import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;

/**
 * Creates a cookie, a small amount of information sent by a server to
 * a Web browser, saved by the browser, and later sent back to the server.
 * A cookie's value can uniquely
 * identify a client, so cookies are commonly used for session management.
 *
 * <p>
 * A cookie has a name, a single value, and optional attributes such as a comment, path and domain
 * qualifiers, a maximum age, and a version number.
 * </p>
 *
 * <p>
 * The server sends cookies to the browser by using the {@link Response#cookie(Cookie)} method,
 * which adds fields to HTTP response headers to send cookies to the browser, one at a time. The
 * browser is expected to support 20 cookies for each Web server, 300 cookies total, and may limit
 * cookie size to 4 KB each.
 * </p>
 *
 * <p>
 * The browser returns cookies to the server by adding fields to HTTP request headers. Cookies can
 * be retrieved from a request by using the {@link Request#cookie(String)} method. Several cookies
 * might have the same name but different path attributes.
 * </p>
 *
 * <p>
 * This class supports both the Version 0 (by Netscape) and Version 1 (by RFC 2109) cookie
 * specifications. By default, cookies are created using Version 0 to ensure the best
 * interoperability.
 * </p>
 *
 * @author edgar and various
 * @since 0.1.0
 */
public interface Cookie {

  /**
   * Build a {@link Cookie}.
   *
   * @author edgar
   * @since 0.1.0
   */
  class Definition {

    /** Cookie's name. */
    private String name;

    /** Cookie's value. */
    private String value;

    /** Cookie's domain. */
    private String domain;

    /** Cookie's path. */
    private String path;

    /** Cookie's comment. */
    private String comment;

    /** HttpOnly flag. */
    private Boolean httpOnly;

    /** True, ensure that the session cookie is only transmitted via HTTPS. */
    private Boolean secure;

    /**
     * By default, <code>-1</code> is returned, which indicates that the cookie will persist until
     * browser shutdown.
     */
    private Integer maxAge;

    /**
     * Creates a new {@link Definition cookie's definition}.
     */
    protected Definition() {
    }

    /**
     * Clone a new {@link Definition cookie's definition}.
     *
     * @param def A cookie's definition.
     */
    public Definition(final Definition def) {
      this.comment = def.comment;
      this.domain = def.domain;
      this.httpOnly = def.httpOnly;
      this.maxAge = def.maxAge;
      this.name = def.name;
      this.path = def.path;
      this.secure = def.secure;
      this.value = def.value;
    }

    /**
     * Creates a new {@link Definition cookie's definition}.
     *
     * @param name Cookie's name.
     * @param value Cookie's value.
     */
    public Definition(final String name, final String value) {
      name(name);
      value(value);
    }

    /**
     * Produces a cookie from current definition.
     *
     * @return A new cookie.
     */
    public Cookie toCookie() {
      return new CookieImpl(this);
    }

    @Override
    public String toString() {
      return toCookie().encode();
    }

    /**
     * Set/Override the cookie's name.
     *
     * @param name A cookie's name.
     * @return This definition.
     */
    public Definition name(final String name) {
      this.name = requireNonNull(name, "A cookie name is required.");
      return this;
    }

    /**
     * @return Cookie's name.
     */
    public Optional<String> name() {
      return Optional.ofNullable(name);
    }

    /**
     * Set the cookie's value.
     *
     * @param value A value.
     * @return This definition.
     */
    public Definition value(final String value) {
      this.value = requireNonNull(value, "A cookie value is required.");
      return this;
    }

    /**
     * @return Cookie's value.
     */
    public Optional<String> value() {
      if (Strings.isNullOrEmpty(value)) {
        return Optional.empty();
      }
      return Optional.of(value);
    }

    /**
     * Set the cookie's domain.
     *
     * @param domain Cookie's domain.
     * @return This definition.
     */
    public Definition domain(final String domain) {
      this.domain = requireNonNull(domain, "A cookie domain is required.");
      return this;
    }

    /**
     * @return A cookie's domain.
     */
    public Optional<String> domain() {
      return Optional.ofNullable(domain);
    }

    /**
     * Set the cookie's path.
     *
     * @param path Cookie's path.
     * @return This definition.
     */
    public Definition path(final String path) {
      this.path = requireNonNull(path, "A cookie path is required.");
      return this;
    }

    /**
     * @return Get cookie's path.
     */
    public Optional<String> path() {
      return Optional.ofNullable(path);
    }

    /**
     * Set cookie's comment.
     *
     * @param comment A cookie's comment.
     * @return This definition.
     */
    public Definition comment(final String comment) {
      this.comment = requireNonNull(comment, "A cookie comment is required.");
      return this;
    }

    /**
     * @return Cookie's comment.
     */
    public Optional<String> comment() {
      return Optional.ofNullable(comment);
    }

    /**
     * Set HttpOnly flag.
     *
     * @param httpOnly True, for HTTP Only.
     * @return This definition.
     */
    public Definition httpOnly(final boolean httpOnly) {
      this.httpOnly = httpOnly;
      return this;
    }

    /**
     * @return HTTP only flag.
     */
    public Optional<Boolean> httpOnly() {
      return Optional.ofNullable(httpOnly);
    }

    /**
     * True, ensure that the session cookie is only transmitted via HTTPS.
     *
     * @param secure True, ensure that the session cookie is only transmitted via HTTPS.
     * @return This definition.
     */
    public Definition secure(final boolean secure) {
      this.secure = secure;
      return this;
    }

    /**
     * @return True, ensure that the session cookie is only transmitted via HTTPS.
     */
    public Optional<Boolean> secure() {
      return Optional.ofNullable(secure);
    }

    /**
     * Sets the maximum age in seconds for this Cookie.
     *
     * <p>
     * A positive value indicates that the cookie will expire after that many seconds have passed.
     * Note that the value is the <i>maximum</i> age when the cookie will expire, not the cookie's
     * current age.
     * </p>
     *
     * <p>
     * A negative value means that the cookie is not stored persistently and will be deleted when
     * the Web browser exits. A zero value causes the cookie to be deleted.
     * </p>
     *
     * @param maxAge an integer specifying the maximum age of the cookie in seconds; if negative,
     *        means the cookie is not stored; if zero, deletes the cookie.
     * @return This definition.
     */
    public Definition maxAge(final int maxAge) {
      this.maxAge = maxAge;
      return this;
    }

    /**
     * Gets the maximum age in seconds for this Cookie.
     *
     * <p>
     * A positive value indicates that the cookie will expire after that many seconds have passed.
     * Note that the value is the <i>maximum</i> age when the cookie will expire, not the cookie's
     * current age.
     * </p>
     *
     * <p>
     * A negative value means that the cookie is not stored persistently and will be deleted when
     * the Web browser exits. A zero value causes the cookie to be deleted.
     * </p>
     * @return Cookie's max age in seconds.
     */
    public Optional<Integer> maxAge() {
      return Optional.ofNullable(maxAge);
    }

  }

  /**
   * Sign cookies using a HMAC algorithm plus SHA-256 hash.
   * Usage:
   *
   * <pre>
   *   String signed = Signature.sign("hello", "mysecretkey");
   *   ...
   *   // is it valid?
   *   assertEquals(signed, Signature.unsign(signed, "mysecretkey");
   * </pre>
   *
   * @author edgar
   * @since 0.1.0
   */
  public class Signature {

    /** Remove trailing '='. */
    private static final Pattern EQ = Pattern.compile("=+$");

    /** Algorithm name. */
    public static final String HMAC_SHA256 = "HmacSHA256";

    /** Signature separator. */
    private static final String SEP = "|";

    /**
     * Sign a value using a secret key. A value and secret key are required. Sign is done with
     * {@link #HMAC_SHA256}.
     * Signed value looks like:
     *
     * <pre>
     *    [raw value] '|' [signed value]
     * </pre>
     *
     * @param value A value to sign.
     * @param secret A secret key.
     * @return A signed value.
     */
    public static String sign(final String value, final String secret) {
      requireNonNull(value, "A value is required.");
      requireNonNull(secret, "A secret is required.");

      try {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(secret.getBytes(), HMAC_SHA256));
        byte[] bytes = mac.doFinal(value.getBytes());
        return value + SEP + EQ.matcher(BaseEncoding.base64().encode(bytes)).replaceAll("");
      } catch (Exception ex) {
        throw new IllegalArgumentException("Can't sing value", ex);
      }
    }

    /**
     * Un-sign a value, previously signed with {@link #sign(String, String)}.
     * Try {@link #valid(String, String)} to check for valid signed values.
     *
     * @param value A signed value.
     * @param secret A secret key.
     * @return A new signed value.
     */
    public static String unsign(final String value, final String secret) {
      requireNonNull(value, "A value is required.");
      requireNonNull(secret, "A secret is required.");
      int sep = value.indexOf(SEP);
      if (sep <= 0) {
        return null;
      }
      String str = value.substring(0, sep);
      String mac = sign(str, secret);

      return mac.equals(value) ? str : null;
    }

    /**
     * True, if the given signed value is valid.
     *
     * @param value A signed value.
     * @param secret A secret key.
     * @return True, if the given signed value is valid.
     */
    public static boolean valid(final String value, final String secret) {
      return unsign(value, secret) != null;
    }

  }

  /**
   * @return Cookie's name.
   */
  String name();

  /**
   * @return Cookie's value.
   */
  Optional<String> value();

  /**
   * @return An optional comment.
   */
  Optional<String> comment();

  /**
   * @return Cookie's domain.
   */
  Optional<String> domain();

  /**
   * Gets the maximum age of this cookie (in seconds).
   *
   * <p>
   * By default, <code>-1</code> is returned, which indicates that the cookie will persist until
   * browser shutdown.
   * </p>
   *
   * @return An integer specifying the maximum age of the cookie in seconds; if negative, means
   *         the cookie persists until browser shutdown
   */
  int maxAge();

  /**
   * @return Cookie's path.
   */
  Optional<String> path();

  /**
   * Returns <code>true</code> if the browser is sending cookies only over a secure protocol, or
   * <code>false</code> if the browser can send cookies using any protocol.
   *
   * @return <code>true</code> if the browser uses a secure protocol, <code>false</code> otherwise.
   */
  boolean secure();

  /**
   * @return True if HTTP Only.
   */
  boolean httpOnly();

  /**
   * @return Encode the cookie.
   */
  String encode();
}

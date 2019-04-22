/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Response cookie implementation. Response are send it back to client using
 * {@link Context#setResponseCookie(Cookie)}.
 *
 * @author edgar
 * @since 2.0.0
 */
public class Cookie {
  private static final DateTimeFormatter fmt = DateTimeFormatter
      .ofPattern("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US)
      .withZone(ZoneId.of("GMT"));

  /** Cookie's name. */
  private String name;

  /** Cookie's value. */
  private String value;

  /** Cookie's domain. */
  private String domain;

  /** Cookie's path. */
  private String path;

  /** HttpOnly flag. */
  private boolean httpOnly;

  /** True, ensure that the session cookie is only transmitted via HTTPS. */
  private boolean secure;

  /**
   * By default, <code>-1</code> is returned, which indicates that the cookie will persist until
   * browser shutdown. In seconds.
   */
  private long maxAge = -1;

  /**
   * Creates a response cookie.
   *
   * @param name Cookie's name.
   * @param value Cookie's value or <code>null</code>.
   */
  public Cookie(@Nonnull String name, @Nullable String value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Creates a response cookie without a value.
   *
   * @param name Cookie's name.
   */
  public Cookie(@Nonnull String name) {
    this(name, null);
  }

  private Cookie(@Nonnull Cookie cookie) {
    this.domain = cookie.domain;
    this.value = cookie.value;
    this.name = cookie.name;
    this.maxAge = cookie.maxAge;
    this.path = cookie.path;
    this.secure = cookie.secure;
    this.httpOnly = cookie.httpOnly;
  }

  /**
   * Copy all state from this cookie and creates a new cookie.
   *
   * @return New cookie.
   */
  public @Nonnull Cookie clone() {
    return new Cookie(this);
  }

  /**
   * Cookie's name.
   *
   * @return Cookie's name.
   */
  public @Nonnull String getName() {
    return name;
  }

  /**
   * Set cookie's name.
   *
   * @param name Cookie's name.
   * @return This cookie.
   */
  public @Nonnull Cookie setName(@Nonnull String name) {
    this.name = name;
    return this;
  }

  /**
   * Cookie's value.
   *
   * @return Cookie's value.
   */
  public @Nullable String getValue() {
    return value;
  }

  /**
   * Set cookie's value.
   *
   * @param value Cookie's value.
   * @return This cookie.
   */
  public @Nonnull Cookie setValue(@Nonnull String value) {
    this.value = value;
    return this;
  }

  /**
   * Cookie's domain.
   *
   * @return Cookie's domain.
   */
  public @Nullable String getDomain() {
    return domain;
  }

  /**
   * Get cookie's domain.
   *
   * @param domain Defaults cookie's domain.
   * @return Cookie's domain..
   */
  public @Nonnull String getDomain(@Nonnull String domain) {
    return this.domain == null ? domain : domain;
  }

  /**
   * Set cookie's domain.
   *
   * @param domain Cookie's domain.
   * @return This cookie.
   */
  public @Nonnull Cookie setDomain(@Nonnull String domain) {
    this.domain = domain;
    return this;
  }

  /**
   * Cookie's path.
   *
   * @return Cookie's path.
   */
  public @Nullable String getPath() {
    return path;
  }

  /**
   * Cookie's path.
   *
   * @param path Defaults path.
   * @return Cookie's path.
   */
  public @Nonnull String getPath(@Nonnull String path) {
    return this.path == null ? path : this.path;
  }

  /**
   * Set cookie's path.
   *
   * @param path Cookie's path.
   * @return This cookie.
   */
  public @Nonnull Cookie setPath(@Nonnull String path) {
    this.path = path;
    return this;
  }

  /**
   * Cookie's http-only flag.
   *
   * @return Htto-only flag.
   */
  public boolean isHttpOnly() {
    return httpOnly;
  }

  /**
   * Set cookie's http-only.
   *
   * @param httpOnly Cookie's http-only.
   * @return This cookie.
   */
  public Cookie setHttpOnly(boolean httpOnly) {
    this.httpOnly = httpOnly;
    return this;
  }

  /**
   * Secure cookie.
   *
   * @return Secure cookie flag.
   */
  public boolean isSecure() {
    return secure;
  }

  /**
   * Set cookie secure flag..
   *
   * @param secure Cookie's secure.
   * @return This cookie.
   */
  public @Nonnull Cookie setSecure(boolean secure) {
    this.secure = secure;
    return this;
  }

  /**
   * Max age value:
   *
   * - <code>-1</code>: indicates a browser session. It is deleted when user closed the browser.
   * - <code>0</code>: indicates a cookie has expired and browser must delete the cookie.
   * - <code>positive value</code>: indicates the number of seconds from current date, where browser
   *   must expires the cookie.
   *
   * @return Max age, in seconds.
   */
  public long getMaxAge() {
    return maxAge;
  }

  /**
   * Set max age value:
   *
   * - <code>-1</code>: indicates a browser session. It is deleted when user closed the browser.
   * - <code>0</code>: indicates a cookie has expired and browser must delete the cookie.
   * - <code>positive value</code>: indicates the number of seconds from current date, where browser
   *   must expires the cookie.
   *
   * @param maxAge Cookie max age.
   * @return This options.
   */
  public @Nonnull Cookie setMaxAge(@Nonnull Duration maxAge) {
    return setMaxAge(maxAge.getSeconds());
  }

  /**
   * Set max age value:
   *
   * - <code>-1</code>: indicates a browser session. It is deleted when user closed the browser.
   * - <code>0</code>: indicates a cookie has expired and browser must delete the cookie.
   * - <code>positive value</code>: indicates the number of seconds from current date, where browser
   *   must expires the cookie.
   *
   * @param maxAge Cookie max age, in seconds.
   * @return This options.
   */
  public @Nonnull Cookie setMaxAge(long maxAge) {
    if (maxAge >= 0) {
      this.maxAge = maxAge;
    } else {
      this.maxAge = -1;
    }
    return this;
  }

  @Override public String toString() {
    StringBuilder buff = new StringBuilder();
    buff.append(name).append("=");
    if (value != null) {
      buff.append(value);
    }
    return buff.toString();
  }

  /**
   * Generates a cookie string. This is the value we sent to the client as <code>Set-Cookie</code>
   * header.
   *
   * @return Cookie string.
   */
  public @Nonnull String toCookieString() {
    StringBuilder sb = new StringBuilder();

    // name = value
    append(sb, name);
    sb.append("=");
    if (value != null) {
      append(sb, value);
    }

    // Path
    if (path != null) {
      sb.append(";Path=");
      append(sb, path);
    }

    // Domain
    if (domain != null) {
      sb.append(";Domain=");
      append(sb, domain);
    }

    // Secure
    if (secure) {
      sb.append(";Secure");
    }

    // HttpOnly
    if (httpOnly) {
      sb.append(";HttpOnly");
    }

    // Max-Age
    if (maxAge >= 0) {
      sb.append(";Max-Age=").append(maxAge);

      /** Old browsers don't support Max-Age. */
      long expires;
      if (maxAge > 0) {
        expires = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxAge);
      } else {
        expires = 0;
      }
      sb.append(";Expires=").append(fmt.format(Instant.ofEpochMilli(expires)));
    }

    return sb.toString();
  }

  private void append(StringBuilder sb, String str) {
    if (needQuote(str)) {
      sb.append('"');
      for (int i = 0; i < str.length(); ++i) {
        char c = str.charAt(i);
        if (c == '"' || c == '\\') {
          sb.append('\\');
        }
        sb.append(c);
      }
      sb.append('"');
    } else {
      sb.append(str);
    }
  }

  private static boolean needQuote(final String value) {
    if (value.length() > 1 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
      return false;
    }

    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      // "\",;\\ \t"
      if (c == '\"' || c == ',' || c == ';' || c == '\\' || c == ' ' || c == '\t') {
        return true;
      }
    }

    return false;
  }
}

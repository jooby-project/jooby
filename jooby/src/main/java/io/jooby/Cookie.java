/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.typesafe.config.Config;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Response cookie implementation. Response are send it back to client using {@link
 * Context#setResponseCookie(Cookie)}.
 *
 * @author edgar
 * @since 2.0.0
 */
public class Cookie {

  /** Algorithm name. */
  public static final String HMAC_SHA256 = "HmacSHA256";

  private static final DateTimeFormatter fmt =
      DateTimeFormatter.ofPattern("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US)
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
   * Value for the 'SameSite' cookie attribute.
   *
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite">
   *     https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite</a>
   */
  private SameSite sameSite;

  /**
   * Creates a response cookie.
   *
   * @param name Cookie's name.
   * @param value Cookie's value or <code>null</code>.
   */
  public Cookie(@NonNull String name, @Nullable String value) {
    this.name = name;
    this.value = value;
  }

  /**
   * Creates a response cookie without a value.
   *
   * @param name Cookie's name.
   */
  public Cookie(@NonNull String name) {
    this(name, null);
  }

  private Cookie(@NonNull Cookie cookie) {
    this.domain = cookie.domain;
    this.value = cookie.value;
    this.name = cookie.name;
    this.maxAge = cookie.maxAge;
    this.path = cookie.path;
    this.secure = cookie.secure;
    this.httpOnly = cookie.httpOnly;
    this.sameSite = cookie.sameSite;
  }

  /**
   * Copy all state from this cookie and creates a new cookie.
   *
   * @return New cookie.
   */
  public @NonNull Cookie clone() {
    return new Cookie(this);
  }

  /**
   * Cookie's name.
   *
   * @return Cookie's name.
   */
  public @NonNull String getName() {
    return name;
  }

  /**
   * Set cookie's name.
   *
   * @param name Cookie's name.
   * @return This cookie.
   */
  public @NonNull Cookie setName(@NonNull String name) {
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
  public @NonNull Cookie setValue(@NonNull String value) {
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
  public @NonNull String getDomain(@NonNull String domain) {
    return this.domain == null ? domain : this.domain;
  }

  /**
   * Set cookie's domain.
   *
   * @param domain Cookie's domain.
   * @return This cookie.
   */
  public @NonNull Cookie setDomain(@NonNull String domain) {
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
  public @NonNull String getPath(@NonNull String path) {
    return this.path == null ? path : this.path;
  }

  /**
   * Set cookie's path.
   *
   * @param path Cookie's path.
   * @return This cookie.
   */
  public @NonNull Cookie setPath(@NonNull String path) {
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
   * Set cookie secure flag.
   *
   * @param secure Cookie's secure.
   * @return This cookie.
   * @throws IllegalArgumentException if {@code false} is specified and the 'SameSite' attribute
   *     value requires a secure cookie.
   */
  public @NonNull Cookie setSecure(boolean secure) {
    if (sameSite != null && sameSite.requiresSecure() && !secure) {
      throw new IllegalArgumentException(
          "Cookies with SameSite="
              + sameSite.getValue()
              + " must be flagged as Secure. Call Cookie.setSameSite(...) with an argument"
              + " allowing non-secure cookies before calling Cookie.setSecure(false).");
    }
    this.secure = secure;
    return this;
  }

  /**
   * Max age value:
   *
   * <p>- <code>-1</code>: indicates a browser session. It is deleted when user closed the browser.
   * - <code>0</code>: indicates a cookie has expired and browser must delete the cookie. - <code>
   * positive value</code>: indicates the number of seconds from current date, where browser must
   * expires the cookie.
   *
   * @return Max age, in seconds.
   */
  public long getMaxAge() {
    return maxAge;
  }

  /**
   * Set max age value:
   *
   * <p>- <code>-1</code>: indicates a browser session. It is deleted when user closed the browser.
   * - <code>0</code>: indicates a cookie has expired and browser must delete the cookie. - <code>
   * positive value</code>: indicates the number of seconds from current date, where browser must
   * expires the cookie.
   *
   * @param maxAge Cookie max age.
   * @return This options.
   */
  public @NonNull Cookie setMaxAge(@NonNull Duration maxAge) {
    return setMaxAge(maxAge.getSeconds());
  }

  /**
   * Set max age value:
   *
   * <p>- <code>-1</code>: indicates a browser session. It is deleted when user closed the browser.
   * - <code>0</code>: indicates a cookie has expired and browser must delete the cookie. - <code>
   * positive value</code>: indicates the number of seconds from current date, where browser must
   * expires the cookie.
   *
   * @param maxAge Cookie max age, in seconds.
   * @return This options.
   */
  public @NonNull Cookie setMaxAge(long maxAge) {
    if (maxAge >= 0) {
      this.maxAge = maxAge;
    } else {
      this.maxAge = -1;
    }
    return this;
  }

  /**
   * Returns the value for the 'SameSite' parameter.
   *
   * <ul>
   *   <li>{@link SameSite#LAX} - Cookies are allowed to be sent with top-level navigations and will
   *       be sent along with GET request initiated by third party website. This is the default
   *       value in modern browsers.
   *   <li>{@link SameSite#STRICT} - Cookies will only be sent in a first-party context and not be
   *       sent along with requests initiated by third party websites.
   *   <li>{@link SameSite#NONE} - Cookies will be sent in all contexts, i.e sending cross-origin is
   *       allowed. Requires the {@code Secure} attribute in latest browser versions.
   *   <li>{@code null} - Not specified.
   * </ul>
   *
   * @return the value for 'SameSite' parameter.
   * @see #setSecure(boolean)
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite">
   *     https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite</a>
   */
  @Nullable public SameSite getSameSite() {
    return sameSite;
  }

  /**
   * Sets the value for the 'SameSite' parameter.
   *
   * <ul>
   *   <li>{@link SameSite#LAX} - Cookies are allowed to be sent with top-level navigations and will
   *       be sent along with GET request initiated by third party website. This is the default
   *       value in modern browsers.
   *   <li>{@link SameSite#STRICT} - Cookies will only be sent in a first-party context and not be
   *       sent along with requests initiated by third party websites.
   *   <li>{@link SameSite#NONE} - Cookies will be sent in all contexts, i.e sending cross-origin is
   *       allowed. Requires the {@code Secure} attribute in latest browser versions.
   *   <li>{@code null} - Not specified.
   * </ul>
   *
   * @param sameSite the value for the 'SameSite' parameter.
   * @return this instance.
   * @throws IllegalArgumentException if a value requiring a secure cookie is specified and this
   *     cookie is not flagged as secure.
   * @see #setSecure(boolean)
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite">
   *     https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite</a>
   */
  public Cookie setSameSite(@Nullable SameSite sameSite) {
    if (sameSite != null && sameSite.requiresSecure() && !isSecure()) {
      throw new IllegalArgumentException(
          "Cookies with SameSite="
              + sameSite.getValue()
              + " must be flagged as Secure. Call Cookie.setSecure(true)"
              + " before calling Cookie.setSameSite(...).");
    }
    this.sameSite = sameSite;
    return this;
  }

  @Override
  public String toString() {
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
  public @NonNull String toCookieString() {
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

    // SameSite
    if (sameSite != null) {
      sb.append(";SameSite=");
      append(sb, sameSite.getValue());
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

  /**
   * Sign a value using a secret key. A value and secret key are required. Sign is done with {@link
   * #HMAC_SHA256}. Signed value looks like:
   *
   * <pre>
   *   [signed value] '|' [raw value]
   * </pre>
   *
   * @param value A value to sign.
   * @param secret A secret key.
   * @return A signed value.
   */
  public static @NonNull String sign(final @NonNull String value, final @NonNull String secret) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(new SecretKeySpec(secret.getBytes(), HMAC_SHA256));
      byte[] bytes = mac.doFinal(value.getBytes());
      return Base64.getEncoder().withoutPadding().encodeToString(bytes) + "|" + value;
    } catch (Exception x) {
      throw SneakyThrows.propagate(x);
    }
  }

  /**
   * Un-sign a value, previously signed with {@link #sign(String, String)}. Produces a nonnull value
   * or <code>null</code> for invalid.
   *
   * @param value A signed value.
   * @param secret A secret key.
   * @return A new signed value or null.
   */
  public static @Nullable String unsign(final @NonNull String value, final @NonNull String secret) {
    int sep = value.indexOf("|");
    if (sep <= 0) {
      return null;
    }
    String str = value.substring(sep + 1);
    return sign(str, secret).equals(value) ? str : null;
  }

  /**
   * Encode a hash into cookie value, like: <code>k1=v1&amp;...&amp;kn=vn</code>. Also, <code>key
   * </code> and <code>value</code> are encoded using {@link URLEncoder}.
   *
   * @param attributes Map to encode.
   * @return URL encoded from map attributes.
   */
  public static @NonNull String encode(@Nullable Map<String, String> attributes) {
    if (attributes == null || attributes.size() == 0) {
      return "";
    }
    try {
      StringBuilder joiner = new StringBuilder();
      String enc = StandardCharsets.UTF_8.name();
      for (Map.Entry<String, String> attribute : attributes.entrySet()) {
        joiner
            .append(URLEncoder.encode(attribute.getKey(), enc))
            .append('=')
            .append(URLEncoder.encode(attribute.getValue(), enc))
            .append('&');
      }
      if (joiner.length() > 0) {
        joiner.setLength(joiner.length() - 1);
      }
      return joiner.toString();
    } catch (UnsupportedEncodingException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  /**
   * Decode a cookie value using, like: <code>k=v</code>, multiple <code>k=v</code> pair are
   * separated by <code>&amp;</code>. Also, <code>k</code> and <code>v</code> are decoded using
   * {@link URLDecoder}.
   *
   * @param value URL encoded value.
   * @return Decoded as map.
   */
  public static @NonNull Map<String, String> decode(@Nullable String value) {
    if (value == null || value.length() == 0) {
      return Collections.emptyMap();
    }
    try {
      Map<String, String> attributes = new HashMap<>();
      String enc = StandardCharsets.UTF_8.name();
      int start = 0;
      int len = value.length();
      do {
        int end = value.indexOf('&', start + 1);
        if (end < 0) {
          end = len;
        }
        // parse attribute
        int eq = value.indexOf('=', start);
        if (eq > 0 && eq < len - 1) {
          attributes.put(
              URLDecoder.decode(value.substring(start, eq), enc),
              URLDecoder.decode(value.substring(eq + 1, end), enc));
        }

        start = end + 1;
      } while (start < len);

      return attributes.isEmpty()
          ? Collections.emptyMap()
          : Collections.unmodifiableMap(attributes);
    } catch (UnsupportedEncodingException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  /**
   * Attempt to create/parse a cookie from application configuration object. The namespace given
   * must be present and must defined a <code>name</code> property.
   *
   * <p>The namespace might optionally defined: value, path, domain, secure, httpOnly and maxAge.
   *
   * @param namespace Cookie namespace/prefix.
   * @param conf Configuration object.
   * @return Parsed cookie or empty.
   */
  public static @NonNull Optional<Cookie> create(@NonNull String namespace, @NonNull Config conf) {
    if (conf.hasPath(namespace)) {
      Cookie cookie = new Cookie(conf.getString(namespace + ".name"));
      value(conf, namespace + ".value", Config::getString, cookie::setValue);
      value(conf, namespace + ".path", Config::getString, cookie::setPath);
      value(conf, namespace + ".domain", Config::getString, cookie::setDomain);
      value(conf, namespace + ".secure", Config::getBoolean, cookie::setSecure);
      value(conf, namespace + ".httpOnly", Config::getBoolean, cookie::setHttpOnly);
      value(
          conf,
          namespace + ".maxAge",
          (c, path) -> c.getDuration(path, TimeUnit.SECONDS),
          cookie::setMaxAge);
      value(
          conf,
          namespace + ".sameSite",
          (c, path) -> SameSite.of(c.getString(path)),
          cookie::setSameSite);
      return Optional.of(cookie);
    }
    return Optional.empty();
  }

  private static <T> void value(
      Config conf, String name, BiFunction<Config, String, T> mapper, Consumer<T> consumer) {
    if (conf.hasPath(name)) {
      consumer.accept(mapper.apply(conf, name));
    }
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

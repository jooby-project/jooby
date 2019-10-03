/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import com.typesafe.config.Config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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

/**
 * Response cookie implementation. Response are send it back to client using
 * {@link Context#setResponseCookie(Cookie)}.
 *
 * @author edgar
 * @since 2.0.0
 */
public class Cookie {

  /** Algorithm name. */
  public static final String HMAC_SHA256 = "HmacSHA256";

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

  /**
   * Sign a value using a secret key. A value and secret key are required. Sign is done with
   * {@link #HMAC_SHA256}.
   * Signed value looks like:
   *
   * <pre>
   *   [signed value] '|' [raw value]
   * </pre>
   *
   * @param value A value to sign.
   * @param secret A secret key.
   * @return A signed value.
   */
  public static @Nonnull String sign(final @Nonnull String value, final @Nonnull String secret) {
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
   * Un-sign a value, previously signed with {@link #sign(String, String)}.
   * Produces a nonnull value or <code>null</code> for invalid.
   *
   * @param value A signed value.
   * @param secret A secret key.
   * @return A new signed value or null.
   */
  public static @Nullable String unsign(final @Nonnull String value, final @Nonnull String secret) {
    int sep = value.indexOf("|");
    if (sep <= 0) {
      return null;
    }
    String str = value.substring(sep + 1);
    return sign(str, secret).equals(value) ? str : null;
  }

  /**
   * Encode a hash into cookie value, like: <code>k1=v1&amp;...&amp;kn=vn</code>. Also,
   * <code>key</code> and <code>value</code> are encoded using {@link URLEncoder}.
   *
   * @param attributes Map to encode.
   * @return URL encoded from map attributes.
   */
  public static @Nonnull String encode(@Nullable Map<String, String> attributes) {
    if (attributes == null || attributes.size() == 0) {
      return "";
    }
    try {
      StringBuilder joiner = new StringBuilder();
      String enc = StandardCharsets.UTF_8.name();
      for (Map.Entry<String, String> attribute : attributes.entrySet()) {
        joiner.append(URLEncoder.encode(attribute.getKey(), enc))
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
  public static @Nonnull Map<String, String> decode(@Nullable String value) {
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
          attributes.put(URLDecoder.decode(value.substring(start, eq), enc),
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
   * The namespace might optionally defined: value, path, domain, secure, httpOnly and maxAge.
   *
   * @param namespace Cookie namespace/prefix.
   * @param conf Configuration object.
   * @return Parsed cookie or empty.
   */
  public static @Nonnull Optional<Cookie> create(@Nonnull String namespace, @Nonnull Config conf) {
    if (conf.hasPath(namespace)) {
      Cookie cookie = new Cookie(conf.getString(namespace + ".name"));
      value(conf, namespace + ".value", Config::getString, cookie::setValue);
      value(conf, namespace + ".path", Config::getString, cookie::setPath);
      value(conf, namespace + ".domain", Config::getString, cookie::setDomain);
      value(conf, namespace + ".secure", Config::getBoolean, cookie::setSecure);
      value(conf, namespace + ".httpOnly", Config::getBoolean, cookie::setHttpOnly);
      value(conf, namespace + ".maxAge", (c, path) -> c.getDuration(path, TimeUnit.SECONDS),
          cookie::setMaxAge);
      return Optional.of(cookie);
    }
    return Optional.empty();
  }

  private static <T> void value(Config conf, String name, BiFunction<Config, String, T> mapper,
      Consumer<T> consumer) {
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

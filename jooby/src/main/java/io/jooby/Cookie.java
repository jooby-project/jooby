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

  public Cookie(@Nonnull String name, @Nullable String value) {
    this.name = name;
    this.value = value;
  }

  public Cookie(@Nonnull String name) {
    this(name, null);
  }

  public Cookie(@Nonnull Cookie cookie) {
    this.domain = cookie.domain;
    this.value = cookie.value;
    this.name = cookie.name;
    this.maxAge = cookie.maxAge;
    this.path = cookie.path;
    this.secure = cookie.secure;
    this.httpOnly = cookie.httpOnly;
  }

  public Cookie clone() {
    return new Cookie(this);
  }

  public @Nonnull String getName() {
    return name;
  }

  public @Nonnull Cookie setName(@Nonnull String name) {
    this.name = name;
    return this;
  }

  public @Nullable String getValue() {
    return value;
  }

  public @Nonnull Cookie setValue(@Nonnull String value) {
    this.value = value;
    return this;
  }

  public @Nullable String getDomain() {
    return domain;
  }

  public @Nonnull String getDomain(@Nonnull String domain) {
    return this.domain == null ? domain : domain;
  }

  public @Nonnull Cookie setDomain(@Nonnull String domain) {
    this.domain = domain;
    return this;
  }

  public @Nullable String getPath() {
    return path;
  }

  public @Nonnull String getPath(@Nonnull String path) {
    return this.path == null ? path : this.path;
  }

  public @Nonnull Cookie setPath(@Nonnull String path) {
    this.path = path;
    return this;
  }

  public boolean isHttpOnly() {
    return httpOnly;
  }

  public Cookie setHttpOnly(boolean httpOnly) {
    this.httpOnly = httpOnly;
    return this;
  }

  public boolean isSecure() {
    return secure;
  }

  public @Nonnull Cookie setSecure(boolean secure) {
    this.secure = secure;
    return this;
  }

  public long getMaxAge() {
    return maxAge;
  }

  public @Nonnull Cookie setMaxAge(long maxAge) {
    this.maxAge = maxAge;
    return this;
  }

  public @Nonnull Cookie setMaxAge(@Nonnull Duration maxAge) {
    return setMaxAge(maxAge.getSeconds());
  }

  @Override public String toString() {
    return name;
  }

  public String toCookieString() {
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

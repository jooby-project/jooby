package org.jooby.internal;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

import org.jooby.Cookie;

public class CookieImpl implements Cookie {

  static final DateTimeFormatter fmt = DateTimeFormatter
      .ofPattern("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.ENGLISH)
      .withZone(ZoneId.of("GMT"));

  private static final String __COOKIE_DELIM = "\",;\\ \t";

  private String name;

  private Optional<String> value;

  private Optional<String> comment;

  private Optional<String> domain;

  private int maxAge;

  private Optional<String> path;

  private boolean secure;

  private boolean httpOnly;

  public CookieImpl(final Cookie.Definition cookie) {
    this.name = cookie.name().get();
    this.value = cookie.value();
    this.comment = cookie.comment();
    this.domain = cookie.domain();
    this.maxAge = cookie.maxAge().orElse(-1);
    this.path = cookie.path();
    this.secure = cookie.secure().orElse(Boolean.FALSE);
    this.httpOnly = cookie.httpOnly().orElse(Boolean.FALSE);
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public Optional<String> value() {
    return value;
  }

  @Override
  public Optional<String> comment() {
    return comment;
  }

  @Override
  public Optional<String> domain() {
    return domain;
  }

  @Override
  public int maxAge() {
    return maxAge;
  }

  @Override
  public Optional<String> path() {
    return path;
  }

  @Override
  public boolean secure() {
    return secure;
  }

  @Override
  public boolean httpOnly() {
    return httpOnly;
  }

  @Override
  public String encode() {
    StringBuilder sb = new StringBuilder();

    Consumer<String> appender = (str) -> {
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
    };

    // name = value
    appender.accept(name());
    sb.append("=");
    value().ifPresent(appender);

    sb.append(";Version=1");

    // Path
    path().ifPresent(path -> {
      sb.append(";Path=");
      appender.accept(path);
    });

    // Domain
    domain().ifPresent(domain -> {
      sb.append(";Domain=");
      appender.accept(domain);
    });

    // Secure
    if (secure()) {
      sb.append(";Secure");
    }

    // HttpOnly
    if (httpOnly()) {
      sb.append(";HttpOnly");
    }

    // Max-Age
    int maxAge = maxAge();
    if (maxAge >= 0) {
      sb.append(";Max-Age=").append(maxAge);

      Instant instant = Instant
          .ofEpochMilli(maxAge > 0 ? System.currentTimeMillis() + maxAge * 1000L : 0);
      sb.append(";Expires=").append(fmt.format(instant));
    }

    // Comment
    comment().ifPresent(comment -> {
      sb.append(";Comment=");
      appender.accept(comment);
    });

    return sb.toString();
  }

  @Override
  public String toString() {
    return encode();
  }

  private static boolean needQuote(final String s) {
    if (s.length() > 1 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
      return false;
    }

    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (__COOKIE_DELIM.indexOf(c) >= 0) {
        return true;
      }

      if (c < 0x20 || c >= 0x7f) {
        throw new IllegalArgumentException("Illegal character fount at: [" + i + "]");
      }
    }

    return false;
  }
}

package jooby.internal;

import static java.util.Objects.requireNonNull;
import jooby.Cookie;

public class MutableCookie implements Cookie {

  private String name;

  private String value;

  private String comment;

  private String domain;

  private int maxAge = -1;

  private String path;

  private boolean secure;

  private int version = 0;

  private boolean isHttpOnly = false;

  public MutableCookie(final String name, final String value) {
    this.name = requireNonNull(name, "The name is required.");
    this.value = requireNonNull(value, "The value is required.");
  }

  /* (non-Javadoc)
   * @see jooby.Cookie#name()
   */
  @Override
  public String name() {
    return name;
  }

  /* (non-Javadoc)
   * @see jooby.Cookie#value()
   */
  @Override
  public String value() {
    return value;
  }

  /* (non-Javadoc)
   * @see jooby.Cookie#comment()
   */
  @Override
  public String comment() {
    return comment;
  }

  public MutableCookie comment(final String comment) {
    this.comment = comment;
    return this;
  }

  /* (non-Javadoc)
   * @see jooby.Cookie#domain()
   */
  @Override
  public String domain() {
    return domain;
  }

  public MutableCookie domain(final String domain) {
    this.domain = domain;
    return this;
  }

  /* (non-Javadoc)
   * @see jooby.Cookie#maxAge()
   */
  @Override
  public int maxAge() {
    return maxAge;
  }

  public MutableCookie maxAge(final int maxAge) {
    this.maxAge = maxAge;
    return this;
  }

  /* (non-Javadoc)
   * @see jooby.Cookie#path()
   */
  @Override
  public String path() {
    return path;
  }

  public MutableCookie path(final String path) {
    this.path = path;
    return this;
  }

  /* (non-Javadoc)
   * @see jooby.Cookie#secure()
   */
  @Override
  public boolean secure() {
    return secure;
  }

  public MutableCookie secure(final boolean secure) {
    this.secure = secure;
    return this;
  }

  /* (non-Javadoc)
   * @see jooby.Cookie#version()
   */
  @Override
  public int version() {
    return version;
  }

  public Cookie version(final int version) {
    this.version = version;
    return this;
  }

  /* (non-Javadoc)
   * @see jooby.Cookie#httpOnly()
   */
  @Override
  public boolean httpOnly() {
    return isHttpOnly;
  }

  public MutableCookie httpOnly(final boolean isHttpOnly) {
    this.isHttpOnly = isHttpOnly;
    return this;
  }

}

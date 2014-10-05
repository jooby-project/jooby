package jooby;

import static java.util.Objects.requireNonNull;

public class SetCookie implements Cookie {

  private String name;

  private String value;

  private String comment;

  private String domain;

  private int maxAge = -1;

  private String path = "/";

  private boolean secure;

  private int version = 0;

  private boolean isHttpOnly = false;

  public SetCookie(final String name, final String value) {
    this.name = requireNonNull(name, "The name is required.");
    this.value = requireNonNull(value, "The value is required.");
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String value() {
    return value;
  }

  @Override
  public String comment() {
    return comment;
  }

  public SetCookie comment(final String comment) {
    this.comment = comment;
    return this;
  }

  @Override
  public String domain() {
    return domain;
  }

  public SetCookie domain(final String domain) {
    this.domain = domain;
    return this;
  }

  @Override
  public int maxAge() {
    return maxAge;
  }

  public SetCookie maxAge(final int maxAge) {
    this.maxAge = maxAge;
    return this;
  }

  @Override
  public String path() {
    return path;
  }

  public SetCookie path(final String path) {
    this.path = path;
    return this;
  }

  @Override
  public boolean secure() {
    return secure;
  }

  public SetCookie secure(final boolean secure) {
    this.secure = secure;
    return this;
  }

  @Override
  public int version() {
    return version;
  }

  public SetCookie version(final int version) {
    this.version = version;
    return this;
  }

  @Override
  public boolean httpOnly() {
    return isHttpOnly;
  }

  public SetCookie httpOnly(final boolean isHttpOnly) {
    this.isHttpOnly = isHttpOnly;
    return this;
  }

}

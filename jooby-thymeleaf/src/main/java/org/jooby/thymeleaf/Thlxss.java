package org.jooby.thymeleaf;

import org.jooby.Env;

class Thlxss {
  private final Env env;

  public Thlxss(final Env env) {
    this.env = env;
  }

  public String escape(final String value, final String xss) {
    return xss(value, xss);
  }

  public String escape(final String value, final String xss1, final String xss2) {
    return xss(value, xss1, xss2);
  }

  public String escape(final String value, final String xss1, final String xss2,
      final String xss3) {
    return xss(value, xss1, xss2, xss3);
  }

  public String escape(final String value, final String xss1, final String xss2, final String xss3,
      final String xss4) {
    return xss(value, xss1, xss2, xss3, xss4);
  }

  public String escape(final String value, final String xss1, final String xss2, final String xss3,
      final String xss4, final String xss5) {
    return xss(value, xss1, xss2, xss3, xss4, xss5);
  }

  private String xss(final String value, final String... xss) {
    return env.xss(xss).apply(value);
  }
}

package org.jooby.jade;

import org.jooby.Env;

public class XssHelper {

  private Env env;

  public XssHelper(final Env env) {
    this.env = env;
  }

  public String apply(final String value, final String... xss) {
    return env.xss(xss).apply(value);
  }

}

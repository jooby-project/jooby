/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import io.jooby.pac4j.Pac4jContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.http.url.UrlResolver;

public class UrlResolverImpl implements UrlResolver {

  private boolean trustProxy;

  public UrlResolverImpl(boolean trustProxy) {
    this.trustProxy = trustProxy;
  }

  @Override public String compute(String path, WebContext context) {
    String requestURL = ((Pac4jContext) context).getContext().getRequestURL(path, trustProxy);
    // no query String
    int i = requestURL.indexOf('?');
    return i > 0 ? requestURL.substring(0, i) : requestURL;
  }
}

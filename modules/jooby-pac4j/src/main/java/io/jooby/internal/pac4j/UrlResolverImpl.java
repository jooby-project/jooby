/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import java.net.URI;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.http.url.UrlResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.pac4j.Pac4jContext;

public class UrlResolverImpl implements UrlResolver {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public String compute(String url, WebContext context) {
    var absoluteURL = isAbsoluteURL(url);
    if (context == null) {
      if (!absoluteURL) {
        log.warn(
            "Unable to resolve URL from path '{}' since no web context was provided. This may"
                + " prevent some authentication clients to work properly. Consider explicitly"
                + " specifying an absolute callback URL or using a custom url resolver.",
            url);
      }

      return url;
    }
    // Rewrite using context which might uses trust proxy setting.
    var path = absoluteURL ? URI.create(url).getPath() : url;
    var requestURL = ((Pac4jContext) context).getContext().getRequestURL(path);
    // no query String
    int i = requestURL.indexOf('?');
    return i > 0 ? requestURL.substring(0, i) : requestURL;
  }

  private static boolean isAbsoluteURL(String url) {
    try {
      return URI.create(url).isAbsolute();
    } catch (Exception ignored) {
      return false;
    }
  }
}

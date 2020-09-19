/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import io.jooby.pac4j.Pac4jContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.http.url.UrlResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

public class UrlResolverImpl implements UrlResolver {

  private static final Pattern HTTP_URL = compile("^https?:.*", CASE_INSENSITIVE);

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override public String compute(String path, WebContext context) {
    if (context == null) {
      if (!HTTP_URL.matcher(path).matches()) {
        log.warn("Unable to resolve URL from path '{}' since no web context was provided." +
            " This may prevent some authentication clients to work properly." +
            " Consider explicitly specifying an absolute callback URL or using a custom url resolver.", path);
      }

      return path;
    }

    String requestURL = ((Pac4jContext) context).getContext().getRequestURL(path);
    // no query String
    int i = requestURL.indexOf('?');
    return i > 0 ? requestURL.substring(0, i) : requestURL;
  }
}

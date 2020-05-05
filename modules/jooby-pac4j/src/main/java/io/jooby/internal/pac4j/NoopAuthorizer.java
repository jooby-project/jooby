/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.context.WebContext;

import java.util.List;

/**
 * We need this to turn off the default
 * {@link org.pac4j.core.authorization.authorizer.CsrfAuthorizer} used it by pac4j when we don't
 * set an authorizer.
 */
public class NoopAuthorizer implements Authorizer {
  public static final String NAME = NoopAuthorizer.class.getName();

  @Override public boolean isAuthorized(WebContext context, List profiles) {
    return true;
  }
}

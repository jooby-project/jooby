/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import org.pac4j.core.context.FrameworkParameters;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.WebContextFactory;

import io.jooby.pac4j.Pac4jContext;
import io.jooby.pac4j.Pac4jFrameworkParameters;

public class ContextFactoryImpl implements WebContextFactory {
  @Override
  public WebContext newContext(FrameworkParameters parameters) {
    if (parameters instanceof Pac4jFrameworkParameters params) {
      return Pac4jContext.create(params.getContext());
    }
    throw new IllegalArgumentException("Can't create context from: " + parameters);
  }
}

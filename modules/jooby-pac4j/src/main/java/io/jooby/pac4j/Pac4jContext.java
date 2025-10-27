/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pac4j;

import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Context;
import io.jooby.internal.pac4j.WebContextImpl;

/**
 * Pac4j web context.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Pac4jContext extends WebContext {
  /**
   * Get underlying context.
   *
   * @return The underlying context.
   */
  Context getContext();

  /**
   * Session store. Required for indirect clients.
   *
   * @return Session store.
   */
  SessionStore getSessionStore();

  /**
   * Wrap a Web context as pac4j context.
   *
   * @param ctx Web context.
   * @return Pac4j web context.
   */
  static Pac4jContext create(@NonNull Context ctx) {
    String key = Pac4jContext.class.getName();
    WebContextImpl impl = ctx.getAttribute(key);
    if (impl == null) {
      impl = new WebContextImpl(ctx);
      ctx.setAttribute(key, impl);
    }
    return impl;
  }
}

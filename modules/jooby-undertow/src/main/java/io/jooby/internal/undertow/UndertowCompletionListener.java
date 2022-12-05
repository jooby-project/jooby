/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import io.jooby.CompletionListeners;
import io.jooby.Route;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;

public class UndertowCompletionListener implements ExchangeCompletionListener {

  private final UndertowContext ctx;
  private CompletionListeners listeners = new CompletionListeners();

  UndertowCompletionListener(UndertowContext ctx) {
    this.ctx = ctx;
  }

  void addListener(Route.Complete listener) {
    listeners.addListener(listener);
  }

  @Override
  public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
    try {
      listeners.run(ctx);
    } finally {
      nextListener.proceed();
    }
  }
}

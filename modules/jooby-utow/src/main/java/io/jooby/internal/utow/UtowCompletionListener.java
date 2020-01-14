package io.jooby.internal.utow;

import io.jooby.CompletionListeners;
import io.jooby.Route;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;

public class UtowCompletionListener implements ExchangeCompletionListener {

  private final UtowContext ctx;
  private CompletionListeners listeners = new CompletionListeners();

  UtowCompletionListener(UtowContext ctx) {
    this.ctx = ctx;
  }

  void addListener(Route.Complete listener) {
    listeners.addListener(listener);
  }

  @Override public void exchangeEvent(HttpServerExchange exchange,
      NextListener nextListener) {
    try {
      listeners.run(ctx);
    } finally {
      nextListener.proceed();
    }
  }
}

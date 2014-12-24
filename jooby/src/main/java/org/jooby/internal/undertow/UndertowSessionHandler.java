package org.jooby.internal.undertow;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;

public class UndertowSessionHandler implements HttpHandler {

  private final UndertowSessionManager sessionManager;

  private final SessionConfig sessionConfig;

  private HttpHandler next;

  public UndertowSessionHandler(final HttpHandler next,
      final UndertowSessionManager sessionManager, final SessionConfig sessionConfig) {
    this.next = next;
    this.sessionConfig = sessionConfig;
    this.sessionManager = sessionManager;
  }

  @Override
  public void handleRequest(final HttpServerExchange exchange) throws Exception {
    exchange.startBlocking(new TmpBlockingHttpExchange(exchange));

    if (exchange.isInIoThread()) {
      exchange.dispatch(this);
      return;
    }

    try {
      UndertowSessionManager.exchange.set(exchange);

      exchange.putAttachment(SessionManager.ATTACHMENT_KEY, sessionManager);
      exchange.putAttachment(SessionConfig.ATTACHMENT_KEY, sessionConfig);

      // update last accessed time
      exchange.addExchangeCompleteListener(
          new UpdateLastAccessTimeListener(sessionConfig, sessionManager));

      // force invalidation by timeout (if any)
      sessionManager.trySession(exchange, sessionConfig);

      next.handleRequest(exchange);

    } finally {
      UndertowSessionManager.exchange.remove();
    }
  }

  public UndertowSessionManager getSessionManager() {
    return sessionManager;
  }

  private static class UpdateLastAccessTimeListener implements ExchangeCompletionListener {

    private final SessionConfig sessionConfig;
    private final SessionManager sessionManager;

    private UpdateLastAccessTimeListener(final SessionConfig sessionConfig,
        final SessionManager sessionManager) {
      this.sessionConfig = sessionConfig;
      this.sessionManager = sessionManager;
    }

    @Override
    public void exchangeEvent(final HttpServerExchange exchange, final NextListener next) {
      try {
        final Session session = sessionManager.getSession(exchange, sessionConfig);
        if (session != null) {
          session.requestDone(exchange);
        }
      } finally {
        next.proceed();
      }
    }
  }

}

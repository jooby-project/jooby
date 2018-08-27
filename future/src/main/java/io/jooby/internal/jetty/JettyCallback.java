package io.jooby.internal.jetty;

import org.eclipse.jetty.util.Callback;

import javax.servlet.AsyncContext;

public class JettyCallback implements Callback {
  private final AsyncContext ctx;

  public JettyCallback(AsyncContext ctx) {
    this.ctx = ctx;
  }

  @Override public void succeeded() {
    ctx.complete();
  }

  @Override public void failed(Throwable x) {
    ctx.complete();
  }
}

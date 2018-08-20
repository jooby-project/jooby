package io.jooby.internal.jetty;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.Callback;

public class JettyCallback implements Callback {
  private final Request request;

  public JettyCallback(Request request) {
    this.request = request;
  }

  @Override public void succeeded() {
    closeIfAsync();
  }

  @Override public void failed(Throwable x) {
    // TODO: print exception
   closeIfAsync();
  }

  private void closeIfAsync() {
    if (request.isAsyncStarted()) {
      request.getAsyncContext().complete();
    }
  }
}

package org.jooby.internal;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Chain;

public class FwdFilter implements Route.Filter{

  private Route.Filter delegate;

  @Override
  public void handle(final Request req, final Response rsp, final Chain chain) throws Exception {
    this.delegate.handle(req, rsp, chain);
  }

  public FwdFilter fwd(final Route.Filter delegate) {
    this.delegate = delegate;
    return this;
  }
}

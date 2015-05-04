package org.jooby.internal.elasticsearch;

import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.rest.RestController;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

public class EmbeddedHandler implements Route.Handler {

  private String path;

  private Supplier<RestController> controller;

  private boolean detailedErrorsEnabled;

  public EmbeddedHandler(final String path, final ManagedNode node, final boolean detailedErrors) {
    this.path = path;
    this.controller = Suppliers.memoize(() ->
        ((InternalNode) node.get()).injector().getInstance(RestController.class));
    detailedErrorsEnabled = detailedErrors;
  }

  @Override
  public void handle(final Request req, final Response rsp) throws Exception {

    EmbeddedHttpRequest restReq = new EmbeddedHttpRequest(path, req);
    EmbeddedHttpChannel channel = new EmbeddedHttpChannel(restReq, rsp, detailedErrorsEnabled);
    controller.get().dispatchRequest(restReq, channel);

    channel.done();
  }

}

package org.jooby.internal;

import java.net.URL;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route.Chain;

public class CdnAssetHandler extends AssetHandler {

  private String cdn;

  public CdnAssetHandler(final String path, final Class<?> loader, final String cdn) {
    super(path, loader);
    this.cdn = cdn;
  }

  @Override
  protected void doHandle(final Request req, final Response rsp, final Chain chain,
      final URL resource) throws Exception {
    String absUrl = cdn + req.path();
    rsp.redirect(absUrl);
  }

}

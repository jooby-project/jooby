package jooby.internal;

import static java.util.Objects.requireNonNull;
import jooby.Asset;
import jooby.HttpStatus;
import jooby.Request;
import jooby.Response;
import jooby.Router;

import com.google.inject.Inject;

public class AssetRoute implements Router {

  private AssetProvider provider;

  @Inject
  public AssetRoute(final AssetProvider provider) {
    this.provider = requireNonNull(provider, "The provider is required.");
  }

  @Override
  public void handle(final Request request, final Response response) throws Exception {
    Asset resource = provider.get(request.path());

    long lastModified = resource.lastModified();

    // Handle if modified since
    if (lastModified > 0) {
      long ifModified = request.header("If-Modified-Since").getLong();
      if (ifModified > 0 && lastModified / 1000 <= ifModified / 1000) {
        response.status(HttpStatus.NOT_MODIFIED);
        return;
      }
      response.header("Last-Modified").setLong(lastModified);
    }
    response.type(resource.type());
    response.send(resource, new AssetBodyConverter(resource.type()));
  }

}

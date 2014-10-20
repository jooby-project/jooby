package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.net.URL;

import javax.inject.Inject;

import jooby.Asset;
import jooby.MediaType;
import jooby.MediaTypeProvider;
import jooby.Response;
import jooby.Route;

class AssetProvider {

  private MediaTypeProvider mediaTypeProvider;

  @Inject
  public AssetProvider(final MediaTypeProvider mediaTypeProvider) {
    this.mediaTypeProvider = requireNonNull(mediaTypeProvider,
        "A file media type provider is required.");
  }

  public Asset get(final String path) throws Exception {
    return resolve(path, mediaTypeProvider.forPath(path));
  }

  private Asset resolve(final String path, final MediaType mediaType) throws Exception {
    URL resource = getClass().getResource(path);
    if (resource == null) {
      File file = new File(path.substring(1));
      if (file.exists()) {
        return new FileAsset(file, mediaType);
      }
      throw new Route.Err(Response.Status.NOT_FOUND, path);
    } else if (resource.getProtocol().equals("file")) {
      return new FileAsset(new File(resource.toURI()), mediaType);
    }

    return new URLAsset(resource, mediaType);
  }

}

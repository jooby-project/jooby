package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.net.URL;

import javax.inject.Inject;

import jooby.Asset;
import jooby.FileMediaTypeProvider;
import jooby.HttpException;
import jooby.HttpStatus;
import jooby.MediaType;

class AssetProvider {

  private FileMediaTypeProvider mediaTypeProvider;

  @Inject
  public AssetProvider(final FileMediaTypeProvider mediaTypeProvider) {
    this.mediaTypeProvider = requireNonNull(mediaTypeProvider,
        "A file media type provider is required.");
  }

  public Asset get(final String path) throws Exception {
    return resolve(path, mediaTypeProvider.typeFor(path));
  }

  private Asset resolve(final String path, final MediaType mediaType) throws Exception {
    URL resource = getClass().getResource(path);
    if (resource == null) {
      File file = new File(path.substring(1));
      if (file.exists()) {
        return new FileAsset(file, mediaType);
      }
      throw new HttpException(HttpStatus.NOT_FOUND, path);
    } else if (resource.getProtocol().equals("file")) {
      return new FileAsset(new File(resource.toURI()), mediaType);
    }

    return new URLAsset(resource, mediaType);
  }

}

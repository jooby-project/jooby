package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import jooby.Asset;
import jooby.MediaType;

import com.google.common.io.Closeables;

class URLAsset implements Asset {

  private URL url;

  private MediaType mediaType;

  private long lastModified;

  public URLAsset(final URL url, final MediaType mediaType) throws IOException {
    this.url = requireNonNull(url, "An url is required.");
    this.mediaType = requireNonNull(mediaType, "A mediaType is required.");
    this.lastModified = lastModified(url);
  }

  @Override
  public String name() {
    return url.getPath();
  }

  @Override
  public InputStream stream() throws IOException {
    return url.openStream();
  }

  @Override
  public long lastModified() {
    return lastModified;
  }

  @Override
  public MediaType type() {
    return mediaType;
  }

  @Override
  public String toString() {
    return name() + "(" + type() + ")";
  }

  private static long lastModified(final URL resource) throws IOException {
    URLConnection uc = null;
    try {
      uc = resource.openConnection();
      return uc.getLastModified();
    } catch (IOException ex) {
      return -1;
    } finally {
      if (uc != null) {
        // http://stackoverflow.com/questions/2057351/how-do-i-get-the-last-modification-time-of
        // -a-java-resource
        Closeables.close(uc.getInputStream(), true);
      }
    }
  }

}

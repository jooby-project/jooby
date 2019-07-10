package io.jooby.internal;

import io.jooby.Asset;
import io.jooby.MediaType;
import io.jooby.SneakyThrows;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * URL asset. Mostly represent a classpath file resource.
 *
 * @author edgar
 * @since 2.0.0
 */
public class URLAsset implements Asset {

  /** URL. */
  private final URL resource;

  /** Path. */
  private final String path;

  /** File size. */
  private long len;

  /** Last modified since or <code>-1</code>. */
  private long lastModified;

  /** Asset content. */
  private InputStream content;

  /**
   * Creates a new URL asset.
   *
   * @param resource Asset resource url.
   * @param path Asset path.
   */
  public URLAsset(@Nonnull URL resource, @Nonnull String path) {
    this.resource = resource;
    this.path = path;
  }

  @Override public long getSize() {
    checkOpen();
    return len;
  }

  @Override public long getLastModified() {
    checkOpen();
    return lastModified;
  }

  @Nonnull @Override public MediaType getContentType() {
    return MediaType.byFile(path);
  }

  @Override public InputStream stream() {
    checkOpen();
    return content;
  }

  @Override public void close() {
    try {
      if (content != null) {
        content.close();
      }
    } catch (Exception x) {
      // NPE when content is a directory
    } finally {
      content = null;
    }
  }

  @Override public boolean equals(Object obj) {
    if (obj instanceof io.jooby.internal.URLAsset) {
      return path.equals(((io.jooby.internal.URLAsset) obj).path);
    }
    return false;
  }

  @Override public int hashCode() {
    return path.hashCode();
  }

  @Override public String toString() {
    return path;
  }

  @Override public boolean isDirectory() {
    return getSize() == 0;
  }

  private void checkOpen() {
    try {
      if (content == null) {
        URLConnection connection = resource.openConnection();
        connection.setUseCaches(false);
        len = connection.getContentLengthLong();
        lastModified = connection.getLastModified();
        content = connection.getInputStream();
      }
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }
}

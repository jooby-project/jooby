/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.FileAsset;
import io.jooby.internal.JarAsset;
import io.jooby.internal.URLAsset;
import sun.net.www.protocol.jar.JarURLConnection;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Represent an static resource file. Asset from file system and classpath are supported.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Asset extends AutoCloseable {

  /**
   * Creates a file system asset.
   *
   * @param resource File resource.
   * @return File resource asset.
   */
  static Asset create(@Nonnull Path resource) {
    return new FileAsset(resource);
  }

  /**
   * Creates a URL asset with the given path.
   * @param path Asset path.
   * @param resource Asset URL.
   * @return URL asset.
   */
  static Asset create(@Nonnull String path, @Nonnull URL resource) {
    try {
      if ("jar".equals(resource.getProtocol())) {
        return new JarAsset((JarURLConnection) resource.openConnection());
      }
      if ("file".equals(resource.getProtocol())) {
        return create(Paths.get(resource.toURI()));
      }
      return new URLAsset(resource, path);
    } catch (IOException | URISyntaxException x) {
      throw SneakyThrows.propagate(x);
    }
  }

  /**
   * @return Asset size (in bytes) or <code>-1</code> if undefined.
   */
  long getSize();

  /**
   * @return The last modified date if possible or -1 when isn't.
   */
  long getLastModified();

  boolean isDirectory();

  /**
   * Computes a weak e-tag value from asset.
   *
   * @return A weak e-tag.
   */
  default @Nonnull String getEtag() {
    StringBuilder b = new StringBuilder(32);
    b.append("W/\"");

    Base64.Encoder encoder = Base64.getEncoder();
    int hashCode = hashCode();
    long lastModified = getLastModified();
    long length = getSize();
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

    buffer.putLong(lastModified ^ hashCode);
    b.append(Long.toHexString(lastModified ^ hashCode));

    buffer.clear();
    buffer.putLong(length ^ hashCode);
    b.append(encoder.encodeToString(buffer.array()));

    b.append('"');
    return b.toString();
  }

  /**
   * @return Asset media type.
   */
  @Nonnull
  MediaType getContentType();

  /**
   * @return Asset content.
   */
  InputStream stream();
}

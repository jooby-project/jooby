/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Represent an static resource file. Asset from file system and classpath are supported.
 *
 * @author edgar
 * @since 2.0.0
 */
public interface Asset {

  /**
   * File system asset.
   *
   * @author edgar
   * @since 2.0.0.
   */
  class FileAsset implements Asset {

    /** File. */
    private Path file;

    /**
     * Creates a new file asset.
     * @param file Asset file.
     */
    public FileAsset(@Nonnull Path file) {
      this.file = file;
    }

    @Override public long getSize() {
      try {
        return Files.size(file);
      } catch (IOException x) {
        throw SneakyThrows.propagate(x);
      }
    }

    @Override public long getLastModified() {
      try {
        return Files.getLastModifiedTime(file).toMillis();
      } catch (IOException x) {
        throw SneakyThrows.propagate(x);
      }
    }

    @Nonnull @Override public MediaType getContentType() {
      return MediaType.byFile(file);
    }

    @Override public InputStream stream() {
      try {
        return new FileInputStream(file.toFile());
      } catch (IOException x) {
        throw SneakyThrows.propagate(x);
      }
    }

    @Override public void release() {
      // NOOP
    }

    @Override public boolean equals(Object obj) {
      if (obj instanceof FileAsset) {
        return file.equals(((FileAsset) obj).file);
      }
      return false;
    }

    @Override public int hashCode() {
      return file.hashCode();
    }

    @Override public String toString() {
      return file.toString();
    }
  }

  /**
   * URL asset. Mostly represent a classpath file resource.
   *
   * @author edgar
   * @since 2.0.0
   */
  class URLAsset implements Asset {

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
    private URLAsset(@Nonnull URL resource, @Nonnull String path) {
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

    @Override public void release() {
      try {
        content.close();
      } catch (IOException | NullPointerException x) {
        // NPE when content is a directory
      }
    }

    @Override public boolean equals(Object obj) {
      if (obj instanceof URLAsset) {
        return path.equals(((URLAsset) obj).path);
      }
      return false;
    }

    @Override public int hashCode() {
      return path.hashCode();
    }

    @Override public String toString() {
      return path;
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
    return new URLAsset(resource, path);
  }

  /**
   * @return Asset size (in bytes) or <code>-1</code> if undefined.
   */
  long getSize();

  /**
   * @return The last modified date if possible or -1 when isn't.
   */
  long getLastModified();

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

  /**
   * Release this asset.
   */
  void release();
}

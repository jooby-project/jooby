/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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

public interface Asset {

  class FileAsset implements Asset {

    private Path file;

    public FileAsset(Path file) {
      this.file = file;
    }

    @Override public long length() {
      try {
        return Files.size(file);
      } catch (IOException x) {
        throw Throwing.sneakyThrow(x);
      }
    }

    @Override public long lastModified() {
      try {
        return Files.getLastModifiedTime(file).toMillis();
      } catch (IOException x) {
        throw Throwing.sneakyThrow(x);
      }
    }

    @Nonnull @Override public MediaType type() {
      return MediaType.byFile(file);
    }

    @Override public InputStream content() {
      try {
        return new FileInputStream(file.toFile());
      } catch (IOException x) {
        throw Throwing.sneakyThrow(x);
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

  class URLAsset implements Asset {

    private final URL resource;

    private final String path;

    private long len;

    private long lastModified;

    private InputStream content;

    private URLAsset(URL resource, String path) {
      this.resource = resource;
      this.path = path;
    }

    @Override public long length() {
      checkOpen();
      return len;
    }

    @Override public long lastModified() {
      checkOpen();
      return lastModified;
    }

    @Nonnull @Override public MediaType type() {
      return MediaType.byFile(path);
    }

    @Override public InputStream content() {
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
        throw Throwing.sneakyThrow(x);
      }
    }
  }

  static Asset create(Path resource) {
    return new FileAsset(resource);
  }

  static Asset create(String path, URL resource) {
    return new URLAsset(resource, path);
  }

  /**
   * @return Asset size (in bytes) or <code>-1</code> if undefined.
   */
  long length();

  /**
   * @return The last modified date if possible or -1 when isn't.
   */
  long lastModified();

  default String etag() {
    StringBuilder b = new StringBuilder(32);
    b.append("W/\"");

    Base64.Encoder encoder = Base64.getEncoder();
    int hashCode = hashCode();
    long lastModified = lastModified();
    long length = length();
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
  MediaType type();

  InputStream content();

  void release();
}

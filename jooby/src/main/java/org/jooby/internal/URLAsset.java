/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.BiConsumer;

import org.jooby.Asset;
import org.jooby.MediaType;

import com.google.common.io.Closeables;

public class URLAsset implements Asset {

  interface Supplier {
    InputStream get() throws IOException;
  }

  private URL url;

  private MediaType mediaType;

  private long lastModified = -1;

  private long length = -1;

  private Supplier stream;

  private String path;

  private boolean exists;

  public URLAsset(final URL url, final String path, final MediaType mediaType) throws Exception {
    this.url = requireNonNull(url, "An url is required.");
    this.path = requireNonNull(path, "Path is required.");
    this.mediaType = requireNonNull(mediaType, "A mediaType is required.");
    this.exists = attr(url, (len, lstMod) -> {
      this.length = len(len);
      this.lastModified = lmod(lstMod);
    });
    this.stream = () -> this.url.openStream();
  }

  @Override
  public String path() {
    return path;
  }

  @Override
  public URL resource() {
    return url;
  }

  @Override
  public long length() {
    return length;
  }

  @Override
  public InputStream stream() throws Exception {
    return stream.get();
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
    return path() + "(" + type() + ")";
  }

  private boolean attr(final URL resource, final BiConsumer<Long, Long> attrs) throws Exception {
    if ("file".equals(resource.getProtocol())) {
      File file = new File(resource.toURI());
      if (file.exists()) {
        attrs.accept(file.length(), file.lastModified());
      }
      return file.isFile();
    } else {
      URLConnection cnn = resource.openConnection();
      cnn.setUseCaches(false);
      attrs.accept(cnn.getContentLengthLong(), cnn.getLastModified());
      try {
        Closeables.closeQuietly(cnn.getInputStream());
      } catch (NullPointerException ex) {
        // dir entries throw NPE :S
        return false;
      }
      return true;
    }
  }

  private static long len(final long value) {
    return value >= 0 ? value : -1;
  }

  private static long lmod(final long value) {
    return value > 0 ? value : -1;
  }

  public boolean exists() {
    return exists;
  }

}

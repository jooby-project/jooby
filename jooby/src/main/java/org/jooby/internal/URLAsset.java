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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.function.BiConsumer;

import org.jooby.Asset;
import org.jooby.MediaType;
import org.jooby.util.ExSupplier;

import com.google.common.io.Closeables;

public class URLAsset implements Asset {

  private URL url;

  private MediaType mediaType;

  private long lastModified = -1;

  private long length = -1;

  private ExSupplier<InputStream> stream;

  private String path;

  public URLAsset(final URL url, final String path, final MediaType mediaType) throws Exception {
    this.url = requireNonNull(url, "An url is required.");
    this.path = requireNonNull(path, "Path is required.");
    this.mediaType = requireNonNull(mediaType, "A mediaType is required.");
    if ("file".equals(url.getProtocol())) {
      File file = new File(url.toURI());
      if (file.exists()) {
        stream = () -> new FileInputStream(file);
        this.length = safeValue(file.length());
        this.lastModified = safeValue(file.lastModified());
      }
    } else {
      headers(url, (len, lstMod) -> {
        this.length = safeValue(len);
        this.lastModified = safeValue(lstMod);
      });
    }
    if (this.stream == null) {
      this.stream = () -> this.url.openStream();
    }
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

  private static void headers(final URL resource, final BiConsumer<Long, Long> callback)
      throws IOException {
    URLConnection uc = null;
    try {
      uc = resource.openConnection();
      uc.setUseCaches(false);
      long len = uc.getContentLengthLong();
      long lastModified = uc.getLastModified();
      callback.accept(len, lastModified);
    } finally {
      if (uc != null) {
        // http://stackoverflow.com/questions/2057351/how-do-i-get-the-last-modification-time-of-a-java-resource
        Closeables.closeQuietly(uc.getInputStream());
      }
    }
  }

  private static long safeValue(final long value) {
    return value > 0 ? value : -1;
  }

}

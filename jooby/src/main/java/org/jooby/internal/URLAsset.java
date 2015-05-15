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

class URLAsset implements Asset {

  private URL url;

  private MediaType mediaType;

  private long lastModified = -1;

  private long length = -1;

  private File file;

  public URLAsset(final URL url, final MediaType mediaType) throws Exception {
    this.url = requireNonNull(url, "An url is required.");
    this.mediaType = requireNonNull(mediaType, "A mediaType is required.");
    if ("file".equals(url.getProtocol())) {
      File file = new File(url.toURI());
      if (file.exists()) {
        this.file = file;
        this.length = file.length();
        this.lastModified = file.lastModified();
      }
    } else {
      headers(url, (len, lstMod) -> {
        this.length = len;
        this.lastModified = lstMod;
      });
    }
  }

  @Override
  public String name() {
    String path = url.getPath();
    int slash = path.lastIndexOf('/');
    return path.substring(slash + 1);
  }

  @Override
  public long length() {
    return length;
  }

  @Override
  public InputStream stream() throws Exception {
    if (file != null) {
      // use OS zero-copy
      return new FileInputStream(file);
    }
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

  private static void headers(final URL resource, final BiConsumer<Long, Long> callback)
      throws IOException {
    URLConnection uc = null;
    try {
      uc = resource.openConnection();
      long len = uc.getContentLengthLong();
      long lastModified = uc.getLastModified();
      callback.accept(len > 0 ? len : -1, lastModified > 0 ? lastModified : -1);
    } finally {
      if (uc != null) {
        // http://stackoverflow.com/questions/2057351/how-do-i-get-the-last-modification-time-of-a-java-resource
        try {
          InputStream stream = uc.getInputStream();
          stream.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

}

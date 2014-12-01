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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.jooby.Asset;
import org.jooby.MediaType;

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
    String path = url.getPath();
    int slash = path.lastIndexOf('/');
    return path.substring(slash + 1);
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

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

import java.io.File;
import java.net.URL;

import org.jooby.Asset;
import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Status;

public class AssetProvider {

  public Asset get(final String path) throws Exception {
    return resolve(path, MediaType.byPath(path).orElse(MediaType.octetstream));
  }

  private Asset resolve(final String path, final MediaType mediaType) throws Exception {
    URL resource = getClass().getResource(path);
    if (resource == null) {
      File file = new File(path.substring(1));
      if (file.exists()) {
        return new FileAsset(file, mediaType);
      }
      throw new Err(Status.NOT_FOUND, path);
    } else if (resource.getProtocol().equals("file")) {
      return new FileAsset(new File(resource.toURI()), mediaType);
    }

    return new URLAsset(resource, mediaType);
  }

}

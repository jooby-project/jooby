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

import java.net.URL;
import java.util.Date;

import org.jooby.Asset;
import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Status;

public class AssetHandler implements Route.Filter {

  @Override
  public void handle(final Request req, final Response rsp, final Route.Chain chain)
      throws Exception {
    String path = req.path();
    Asset resource = resolve(path, MediaType.byPath(path).orElse(MediaType.octetstream));

    long lastModified = resource.lastModified();

    // Handle if modified since
    if (lastModified > 0) {
      long ifModified = req.header("If-Modified-Since").toOptional(Long.class).orElse(-1l);
      if (ifModified > 0 && lastModified / 1000 <= ifModified / 1000) {
        rsp.status(Status.NOT_MODIFIED).end();
        return;
      }
      rsp.header("Last-Modified", new Date(lastModified));
    }
    long length = resource.length();
    if (length >= 0) {
      rsp.length(length);
    }
    rsp.type(resource.type());
    rsp.send(resource);
  }

  private Asset resolve(final String path, final MediaType mediaType) throws Exception {
    URL resource = getClass().getResource(path);
    if (resource == null) {
      throw new Err(Status.NOT_FOUND, path);
    }

    return new URLAsset(resource, mediaType);
  }
}

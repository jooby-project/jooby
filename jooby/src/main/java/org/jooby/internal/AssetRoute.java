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

import java.util.Date;

import org.jooby.Asset;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Status;

import com.google.inject.Inject;

public class AssetRoute implements Route.Handler {

  private AssetProvider provider;

  @Inject
  public AssetRoute(final AssetProvider provider) {
    this.provider = requireNonNull(provider, "Asset provider is required.");
  }

  @Override
  public void handle(final Request request, final Response response) throws Exception {
    Asset resource = provider.get(request.path());

    long lastModified = resource.lastModified();

    // Handle if modified since
    if (lastModified > 0) {
      long ifModified = request.header("If-Modified-Since").toOptional(Long.class).orElse(-1l);
      if (ifModified > 0 && lastModified / 1000 <= ifModified / 1000) {
        response.status(Status.NOT_MODIFIED);
        return;
      }
      response.header("Last-Modified", new Date(lastModified));
    }
    response.type(resource.type());
    response.send(resource);
  }

}

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
import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;
import java.util.function.BiFunction;

import org.jooby.Asset;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Chain;
import org.jooby.Status;

public class AssetHandler implements Route.Filter {

  private BiFunction<Request, String, String> fn;

  private Class<?> loader;

  public AssetHandler(final String path, final Class<?> loader) {
    String pattern = RoutePattern.normalize(path);
    this.fn = pattern.equals("/")
        ? (req, p) -> p
        : (req, p) -> {
          return MessageFormat.format(pattern, vars(req));
        };
    this.loader = loader;
  }

  @Override
  public void handle(final Request req, final Response rsp, final Route.Chain chain)
      throws Exception {
    String path = req.path();
    URL resource = resolve(req, path);

    if (resource == null) {
      // ignore and move next;
      chain.next(req, rsp);
      return;
    }

    doHandle(req, rsp, chain, resource);
  }

  protected void doHandle(final Request req, final Response rsp, final Chain chain,
      final URL resource) throws Exception {

    Asset asset = new URLAsset(resource, req.path(),
        MediaType.byPath(resource.getPath()).orElse(MediaType.octetstream));

    String etag = asset.etag();
    long lastModified = asset.lastModified();

    boolean ifnm = req.header("If-None-Match").toOptional()
        .map(etag::equals)
        .orElse(false);
    // handle etag
    if (ifnm) {
      rsp.header("ETag", etag).status(Status.NOT_MODIFIED).end();
      return;
    }

    // Handle if modified since
    if (lastModified > 0) {
      boolean ifm = req.header("If-Modified-Since").toOptional(Long.class)
          .map(ifModified -> lastModified / 1000 <= ifModified / 1000)
          .orElse(false);
      if (ifm) {
        rsp.status(Status.NOT_MODIFIED).end();
        return;
      }
      rsp.header("Last-Modified", new Date(lastModified));
    }
    long length = asset.length();
    if (length >= 0) {
      rsp.length(length);
    }
    rsp.header("ETag", etag)
        .send(asset);

  }

  private URL resolve(final Request req, final String path) throws Exception {
    String target = fn.apply(req, path);
    return loader.getResource(target);
  }

  private static Object[] vars(final Request req) {
    Map<Object, String> vars = req.route().vars();
    return vars.values().toArray(new Object[vars.size()]);
  }
}

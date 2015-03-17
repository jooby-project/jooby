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
    Asset resource = resolve(req, path);

    if (resource == null) {
      // ignore and move next;
      chain.next(req, rsp);
      return;
    }

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

  private Asset resolve(final Request req, final String path) throws Exception {
    String target = fn.apply(req, path);
    URL resource = loader.getResource(target);
    if (resource == null) {
      return null;
    }

    return new URLAsset(resource, MediaType.byPath(target).orElse(MediaType.octetstream));
  }

  private static Object[] vars(final Request req) {
    Map<Object, String> vars = req.route().vars();
    return vars.values().toArray(new Object[vars.size()]);
  }
}

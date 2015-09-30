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
package org.jooby.handlers;

import static java.util.Objects.requireNonNull;

import java.net.URL;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;
import java.util.function.BiFunction;

import org.jooby.Asset;
import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Status;
import org.jooby.internal.RoutePattern;
import org.jooby.internal.URLAsset;

import com.google.common.base.Strings;

/**
 * Serve static resources, via {@link Jooby#assets(String)} or variants.
 *
 * <h1>e-tag support</h1>
 * <p>
 * It generates <code>ETag</code> headers using {@link Asset#etag()}. It handles
 * <code>If-None-Match</code> header automatically.
 * </p>
 * <p>
 * <code>ETag</code> handling is enabled by default. If you want to disabled etag support
 * {@link #etag(boolean)}.
 * </p>
 *
 * <h1>modified since support</h1>
 * <p>
 * It generates <code>Last-Modified</code> header using {@link Asset#lastModified()}. It handles
 * <code>If-Modified-Since</code> header automatically.
 * </p>
 *
 * <h1>CDN support</h1>
 * <p>
 * Asset can be serve from a content delivery network (a.k.a cdn). All you have to do is to set the
 * <code>assets.cdn</code> property.
 * </p>
 *
 * <pre>
 * assets.cdn = "http://http://d7471vfo50fqt.cloudfront.net"
 * </pre>
 *
 * <p>
 * Resolved assets are redirected to the cdn.
 * </p>
 *
 * @author edgar
 * @since 0.1.0
 */
public class AssetHandler implements Route.Handler {

  private BiFunction<Request, String, String> fn;

  private Class<?> loader;

  private String cdn;

  private boolean etag = true;

  private long maxAge = -1;

  /**
   * <p>
   * Creates a new {@link AssetHandler}. The handler accepts a location pattern, that serve for
   * locating the static resource.
   * </p>
   *
   * Given <code>assets("/assets/**", "/")</code> with:
   *
   * <pre>
   *   GET /assets/js/index.js it translates the path to: /assets/js/index.js
   * </pre>
   *
   * Given <code>assets("/js/**", "/assets")</code> with:
   *
   * <pre>
   *   GET /js/index.js it translate the path to: /assets/js/index.js
   * </pre>
   *
   * Given <code>assets("/webjars/**", "/META-INF/resources/webjars/{0}")</code> with:
   *
   * <pre>
   *   GET /webjars/jquery/2.1.3/jquery.js it translate the path to: /META-INF/resources/webjars/jquery/2.1.3/jquery.js
   * </pre>
   *
   * @param pattern Pattern to locate static resources.
   * @param loader The one who load the static resources.
   */
  public AssetHandler(final String pattern, final Class<?> loader) {
    init(RoutePattern.normalize(pattern), loader);
  }

  /**
   * <p>
   * Creates a new {@link AssetHandler}. The location pattern can be one of.
   * </p>
   *
   * Given <code>/</code> like in <code>assets("/assets/**", "/")</code> with:
   *
   * <pre>
   *   GET /assets/js/index.js it translates the path to: /assets/js/index.js
   * </pre>
   *
   * Given <code>/assets</code> like in <code>assets("/js/**", "/assets")</code> with:
   *
   * <pre>
   *   GET /js/index.js it translate the path to: /assets/js/index.js
   * </pre>
   *
   * Given <code>/META-INF/resources/webjars/{0}</code> like in
   * <code>assets("/webjars/**", "/META-INF/resources/webjars/{0}")</code> with:
   *
   * <pre>
   *   GET /webjars/jquery/2.1.3/jquery.js it translate the path to: /META-INF/resources/webjars/jquery/2.1.3/jquery.js
   * </pre>
   *
   * @param pattern Pattern to locate static resources.
   */
  public AssetHandler(final String pattern) {
    init(RoutePattern.normalize(pattern), getClass());
  }

  /**
   * @param etag Turn on/off etag support.
   * @return This handler.
   */
  public AssetHandler etag(final boolean etag) {
    this.etag = etag;
    return this;
  }

  /**
   * @param cdn If set, every resolved asset will be serve from it.
   * @return This handler.
   */
  public AssetHandler cdn(final String cdn) {
    this.cdn = Strings.emptyToNull(cdn);
    return this;
  }

  /**
   * @param maxAge Set the cache header max-age value in seconds.
   * @return This handler.
   */
  public AssetHandler maxAge(final long maxAge) {
    this.maxAge = maxAge;
    return this;
  }

  @Override
  public void handle(final Request req, final Response rsp)
      throws Exception {
    String path = req.path();
    URL resource = resolve(req, path);

    if (resource != null) {
      // cdn?
      if (cdn != null) {
        String absUrl = cdn + req.path();
        rsp.redirect(absUrl);
        rsp.end();
      } else {
        doHandle(req, rsp, resource);
      }
    }
  }

  private void doHandle(final Request req, final Response rsp,
      final URL resource) throws Exception {

    Asset asset = new URLAsset(resource, req.path(),
        MediaType.byPath(resource.getPath()).orElse(MediaType.octetstream));

    // handle etag
    if (this.etag) {
      String etag = asset.etag();
      boolean ifnm = req.header("If-None-Match").toOptional()
          .map(etag::equals)
          .orElse(false);
      if (ifnm) {
        rsp.header("ETag", etag).status(Status.NOT_MODIFIED).end();
        return;
      }

      rsp.header("ETag", etag);
    }

    // Handle if modified since
    long lastModified = asset.lastModified();
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

    // cache max-age
    if (maxAge > 0) {
      rsp.header("Cache-Control", "max-age=" + maxAge);
    }

    send(req, rsp, asset);
  }

  /**
   * Send an asset to the client.
   *
   * @param req Request.
   * @param rsp Response.
   * @param asset Resolve asset.
   * @throws Exception If send fails.
   */
  protected void send(final Request req, final Response rsp, final Asset asset)
      throws Exception {
    rsp.send(asset);
  }

  private URL resolve(final Request req, final String path) throws Exception {
    String target = fn.apply(req, path);
    return resolve(target);
  }

  /**
   * Resolve a path as a {@link URL}.
   *
   * @param path Path of resource to resolve.
   * @return A URL or <code>null</code> for unresolved resource.
   * @throws Exception If something goes wrong.
   */
  protected URL resolve(final String path) throws Exception {
    return loader.getResource(path);
  }

  private void init(final String pattern, final Class<?> loader) {
    requireNonNull(loader, "Resource loader is required.");
    this.fn = pattern.equals("/")
        ? (req, p) -> p
        : (req, p) -> MessageFormat.format(pattern, vars(req));
    this.loader = loader;
  }

  private static Object[] vars(final Request req) {
    Map<Object, String> vars = req.route().vars();
    return vars.values().toArray(new Object[vars.size()]);
  }

}

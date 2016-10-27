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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

import org.jooby.Asset;
import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Status;
import org.jooby.internal.URLAsset;

import com.google.common.base.Strings;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import javaslang.Function1;
import javaslang.Function2;
import javaslang.control.Try;

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
 * assets.cdn = "http://d7471vfo50fqt.cloudfront.net"
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

  private static final Function1<ClassLoader, ClassLoader> cloader = loader().memoized();

  private static final Function1<String, String> prefix = prefix().memoized();

  private Function2<Request, String, String> fn;

  private ClassLoader loader;

  private String cdn;

  private boolean etag = true;

  private long maxAge = -1;

  private boolean lastModified = true;

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
  public AssetHandler(final String pattern, final ClassLoader loader) {
    init(Route.normalize(pattern), loader);
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
    init(Route.normalize(pattern), getClass().getClassLoader());
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
   * @param enabled Turn on/off last modified support.
   * @return This handler.
   */
  public AssetHandler lastModified(final boolean enabled) {
    this.lastModified = enabled;
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
   * @param maxAge Set the cache header max-age value.
   * @return This handler.
   */
  public AssetHandler maxAge(final Duration maxAge) {
    return maxAge(maxAge.getSeconds());
  }

  /**
   * @param maxAge Set the cache header max-age value in seconds.
   * @return This handler.
   */
  public AssetHandler maxAge(final long maxAge) {
    this.maxAge = maxAge;
    return this;
  }

  /**
   * Parse value as {@link Duration}. If the value is already a number then it uses as seconds.
   * Otherwise, it parse expressions like: 8m, 1h, 365d, etc...
   *
   * @param maxAge Set the cache header max-age value in seconds.
   * @return This handler.
   */
  public AssetHandler maxAge(final String maxAge) {
    Try.of(() -> Long.parseLong(maxAge))
        .recover(x -> ConfigFactory.empty()
            .withValue("v", ConfigValueFactory.fromAnyRef(maxAge))
            .getDuration("v")
            .getSeconds())
        .onSuccess(this::maxAge);
    return this;
  }

  @Override
  public void handle(final Request req, final Response rsp) throws Throwable {
    String path = req.path();
    URL resource = resolve(req, path);

    if (resource != null) {
      String localpath = resource.getPath();
      int jarEntry = localpath.indexOf("!/");
      if (jarEntry > 0) {
        localpath = localpath.substring(jarEntry + 2);
      }

      URLAsset asset = new URLAsset(resource, path,
          MediaType.byPath(localpath).orElse(MediaType.octetstream));

      if (asset.exists()) {
        // cdn?
        if (cdn != null) {
          String absUrl = cdn + path;
          rsp.redirect(absUrl);
          rsp.end();
        } else {
          doHandle(req, rsp, asset);
        }
      }
    }
  }

  private void doHandle(final Request req, final Response rsp, final Asset asset) throws Throwable {

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
    if (this.lastModified) {
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
  protected void send(final Request req, final Response rsp, final Asset asset) throws Throwable {
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

  private void init(final String pattern, final ClassLoader loader) {
    requireNonNull(loader, "Resource loader is required.");
    this.fn = pattern.equals("/")
        ? (req, p) -> prefix.apply(p)
        : (req, p) -> MessageFormat.format(prefix.apply(pattern), vars(req));
    this.loader = cloader.apply(loader);
  }

  private static Object[] vars(final Request req) {
    Map<Object, String> vars = req.route().vars();
    return vars.values().toArray(new Object[vars.size()]);
  }

  private static Function1<ClassLoader, ClassLoader> loader() {
    return parent -> {
      File publicDir = new File("public");
      if (publicDir.exists()) {
        try {
          return new URLClassLoader(new URL[]{publicDir.toURI().toURL() }, null) {
            @Override
            public URL getResource(final String name) {
              URL url = findResource(name);
              if (url == null) {
                url = parent.getResource(name);
              }
              return url;
            };
          };
        } catch (MalformedURLException ex) {
          // shh
        }
      }
      return parent;
    };
  }

  private static Function1<String, String> prefix() {
    return p -> p.substring(1);
  }

}

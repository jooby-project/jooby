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

import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jooby.Deferred;
import org.jooby.Err;
import org.jooby.Err.Handler;
import org.jooby.MediaType;
import org.jooby.Renderer;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.Sse;
import org.jooby.Status;
import org.jooby.WebSocket;
import org.jooby.WebSocket.Definition;
import org.jooby.internal.parser.ParserExecutor;
import org.jooby.spi.HttpHandler;
import org.jooby.spi.NativeRequest;
import org.jooby.spi.NativeResponse;
import org.jooby.spi.NativeWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

import javaslang.control.Try;

@Singleton
public class HttpHandlerImpl implements HttpHandler {

  private static class RouteKey {
    protected String method;

    protected String path;

    protected MediaType consumes;

    protected List<MediaType> produces;

    private int hc;

    public RouteKey(final String method, final String path, final MediaType consumes,
        final List<MediaType> produces) {
      this.method = method;
      this.path = path;
      this.consumes = consumes;
      this.produces = produces;
      hc = 1;
      hc = 31 * hc + method.hashCode();
      hc = 31 * hc + path.hashCode();
      hc = 31 * hc + consumes.hashCode();
      hc = 31 * hc + produces.hashCode();
    }

    @Override
    public int hashCode() {
      return hc;
    }

    @Override
    public boolean equals(final Object obj) {
      RouteKey that = (RouteKey) obj;
      return method.equals(that.method) && path.equals(that.path) && produces.equals(that.produces)
          && consumes.equals(that.consumes);
    }

  }

  private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

  private static final String WEB_SOCKET = "WebSocket";

  private static final String UPGRADE = "Upgrade";

  private static final String REFERER = "Referer";

  private static final String PATH = "path";

  private static final String CONTEXT_PATH = "contextPath";

  private static final Key<Request> REQ = Key.get(Request.class);

  private static final Key<Response> RSP = Key.get(Response.class);

  private static final Key<Sse> SSE = Key.get(Sse.class);

  private static final Key<Session> SESS = Key.get(Session.class);

  private static final Key<String> DEF_EXEC = Key.get(String.class, Names.named("deferred"));

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(HttpHandler.class);

  private Injector injector;

  private Set<Err.Handler> err;

  private String applicationPath;

  private RequestScope requestScope;

  private Set<Definition> socketDefs;

  private Config config;

  private int port;

  private String _method;

  private Charset charset;

  private List<Renderer> renderers;

  private ParserExecutor parserExecutor;

  private List<Locale> locale;

  private final LoadingCache<RouteKey, List<Route>> routeCache;

  private final String redirectHttps;

  private Function<String, String> rpath = null;

  private String contextPath;

  private boolean hasSockets;

  private final Map<String, Renderer> rendererMap;

  private StatusCodeProvider sc;

  /** Global deferred executor. */
  private Key<Executor> gexec;

  @Inject
  public HttpHandlerImpl(final Injector injector,
      final RequestScope requestScope,
      final Set<Route.Definition> routes,
      final Set<WebSocket.Definition> sockets,
      final @Named("application.path") String path,
      final ParserExecutor parserExecutor,
      final Set<Renderer> renderers,
      final Set<Err.Handler> err,
      final StatusCodeProvider sc,
      final Charset charset,
      final List<Locale> locale) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.requestScope = requireNonNull(requestScope, "A request scope is required.");
    this.socketDefs = requireNonNull(sockets, "Sockets are required.");
    this.hasSockets = socketDefs.size() > 0;
    this.applicationPath = normalizeURI(requireNonNull(path, "An application.path is required."));
    this.err = requireNonNull(err, "An err handler is required.");
    this.sc = sc;
    this.config = injector.getInstance(Config.class);
    _method = Strings.emptyToNull(this.config.getString("server.http.Method").trim());
    this.port = config.getInt("application.port");
    this.charset = charset;
    this.locale = locale;
    this.parserExecutor = parserExecutor;
    this.renderers = ImmutableList.copyOf(renderers);
    rendererMap = new HashMap<>();
    this.renderers.forEach(r -> rendererMap.put(r.name(), r));

    // route cache
    routeCache = routeCache(routes, config);
    // force https
    String redirectHttps = config.getString("application.redirect_https").trim();
    this.redirectHttps = redirectHttps.length() > 0 ? redirectHttps : null;

    // custom path?
    if (applicationPath.equals("/")) {
      this.contextPath = "";
    } else {
      this.contextPath = applicationPath;
      this.rpath = rootpath(applicationPath);
    }
    // global deferred executor
    this.gexec = Key.get(Executor.class, Names.named(injector.getInstance(DEF_EXEC)));
  }

  @Override
  public void handle(final NativeRequest request, final NativeResponse response) throws Exception {
    Map<String, Object> locals = new HashMap<>(16);

    Map<Object, Object> scope = new HashMap<>(16);

    String verb = (_method == null ? request.method() : method(_method, request)).toUpperCase();
    String requestPath = normalizeURI(request.path());
    if (rpath != null) {
      requestPath = rpath.apply(requestPath);
    }

    // default locals
    locals.put(CONTEXT_PATH, contextPath);
    locals.put(PATH, requestPath);

    final String path = verb + requestPath;

    Route notFound = RouteImpl.notFound(verb, path, MediaType.ALL);

    RequestImpl req = new RequestImpl(injector, request, contextPath, port, notFound, charset,
        locale, scope, locals);

    ResponseImpl rsp = new ResponseImpl(req, parserExecutor, response, notFound, renderers,
        rendererMap, locals, req.charset(), request.header(REFERER));

    MediaType type = req.type();

    // seed req & rsp
    scope.put(REQ, req);
    scope.put(RSP, rsp);

    // seed sse
    Provider<Sse> sse = () -> Try.of(() -> request.upgrade(Sse.class))
        .getOrElseThrow(() -> new UnsupportedOperationException("Server-sent events"));
    scope.put(SSE, sse);

    // seed session
    Provider<Session> session = () -> req.session();
    scope.put(SESS, session);

    boolean deferred = false;
    Throwable x = null;
    try {

      requestScope.enter(scope);

      // force https?
      if (redirectHttps != null) {
        if (!req.secure()) {
          rsp.redirect(MessageFormat.format(redirectHttps, requestPath.substring(1)));
          return;
        }
      }

      // websocket?
      if (hasSockets) {
        if (upgrade(request)) {
          Optional<WebSocket> sockets = findSockets(socketDefs, requestPath);
          if (sockets.isPresent()) {
            NativeWebSocket ws = request.upgrade(NativeWebSocket.class);
            ws.onConnect(() -> ((WebSocketImpl) sockets.get()).connect(injector, req, ws));
            return;
          }
        }
      }

      // usual req/rsp
      List<Route> routes = routeCache
          .getUnchecked(new RouteKey(verb, requestPath, type, req.accept()));

      new RouteChain(req, rsp, routes).next(req, rsp);

    } catch (DeferredExecution ex) {
      deferred = true;
      onDeferred(scope, request, req, rsp, ex.deferred);
    } catch (Throwable ex) {
      x = ex;
    } finally {
      cleanup(req, rsp, true, x, !deferred);
    }
  }

  private boolean upgrade(final NativeRequest request) {
    Optional<String> upgrade = request.header(UPGRADE);
    return upgrade.isPresent() && upgrade.get().equalsIgnoreCase(WEB_SOCKET);
  }

  private void done(final RequestImpl req, final ResponseImpl rsp, final Throwable x,
      final boolean close) {
    // mark request/response as done.
    req.done();
    if (close) {
      rsp.done(Optional.ofNullable(x));
    }
  }

  private void onDeferred(final Map<Object, Object> scope, final NativeRequest request,
      final RequestImpl req, final ResponseImpl rsp, final Deferred deferred) {
    try {
      /** Deferred executor. */
      Key<Executor> execKey = deferred.executor()
          .map(it -> Key.get(Executor.class, Names.named(it)))
          .orElse(gexec);

      /** Get executor. */
      Executor executor = injector.getInstance(execKey);

      request.startAsync(executor, () -> {
        try {
          deferred.handler(req, (success, x) -> {
            boolean close = false;
            Optional<Throwable> failure = Optional.ofNullable(x);
            try {
              requestScope.enter(scope);
              if (success != null) {
                close = true;
                rsp.send(success);
              }
            } catch (Throwable exerr) {
              failure = Optional.of(failure.orElse(exerr));
            } finally {
              Throwable cause = failure.orElse(null);
              if (cause != null) {
                close = true;
                handleErr(req, rsp, cause);
              }
              cleanup(req, rsp, close, cause, true);
            }
          });
        } catch (Exception ex) {
          handleErr(req, rsp, ex);
        }
      });
    } catch (Exception ex) {
      handleErr(req, rsp, ex);
    }
  }

  private void cleanup(final RequestImpl req, final ResponseImpl rsp, final boolean close,
      final Throwable x, final boolean done) {
    if (x != null) {
      handleErr(req, rsp, x);
    }
    if (done) {
      done(req, rsp, x, close);
    }
    requestScope.exit();
  }

  private void handleErr(final RequestImpl req, final ResponseImpl rsp, final Throwable ex) {
    try {
      log.debug("execution of: {}{} resulted in exception", req.method(), req.path(), ex);

      rsp.reset();

      // execution failed, find status code
      Status status = sc.apply(ex);

      rsp.header("Cache-Control", NO_CACHE);
      rsp.status(status);

      Err err = ex instanceof Err ? (Err) ex : new Err(status, ex);

      Iterator<Handler> it = this.err.iterator();
      while (!rsp.committed() && it.hasNext()) {
        Err.Handler next = it.next();
        log.debug("handling err with: {}", next);
        next.handle(req, rsp, err);
      }
    } catch (Throwable errex) {
      log.error("execution of: {}{} resulted in exception\n{}Caused by:",
          req.method(), req.path(), Throwables.getStackTraceAsString(errex), ex);
    }
  }

  private static String normalizeURI(final String uri) {
    int len = uri.length();
    return len > 1 && uri.charAt(len - 1) == '/' ? uri.substring(0, len - 1) : uri;
  }

  private static List<Route> routes(final Set<Route.Definition> routeDefs, final String method,
      final String path, final MediaType type, final List<MediaType> accept) {
    List<Route> routes = findRoutes(routeDefs, method, path, type, accept);

    routes.add(RouteImpl.fromStatus((req, rsp, chain) -> {
      if (!rsp.status().isPresent()) {
        // 406 or 415
        Err ex = handle406or415(routeDefs, method, path, type, accept);
        if (ex != null) {
          throw ex;
        }
        // 405
        ex = handle405(routeDefs, method, path, type, accept);
        if (ex != null) {
          throw ex;
        }
        throw new Err(Status.NOT_FOUND, req.path(true));
      }
    }, method, path, "err", accept));

    return routes;
  }

  private static List<Route> findRoutes(final Set<Route.Definition> routeDefs, final String method,
      final String path, final MediaType type, final List<MediaType> accept) {

    List<Route> routes = new ArrayList<>();
    for (Route.Definition routeDef : routeDefs) {
      Optional<Route> route = routeDef.matches(method, path, type, accept);
      if (route.isPresent()) {
        routes.add(route.get());
      }
    }
    return routes;
  }

  private static Optional<WebSocket> findSockets(final Set<WebSocket.Definition> sockets,
      final String path) {
    for (WebSocket.Definition socketDef : sockets) {
      Optional<WebSocket> match = socketDef.matches(path);
      if (match.isPresent()) {
        return match;
      }
    }
    return Optional.empty();
  }

  private static Err handle405(final Set<Route.Definition> routeDefs, final String method,
      final String path, final MediaType type, final List<MediaType> accept) {

    if (alternative(routeDefs, method, path).size() > 0) {
      return new Err(Status.METHOD_NOT_ALLOWED, method);
    }

    return null;
  }

  private static List<Route> alternative(final Set<Route.Definition> routeDefs, final String verb,
      final String uri) {
    List<Route> routes = new LinkedList<>();
    Set<String> verbs = Sets.newHashSet(Route.METHODS);
    verbs.remove(verb);
    for (String alt : verbs) {
      findRoutes(routeDefs, alt, uri, MediaType.all, MediaType.ALL)
          .stream()
          // skip glob pattern
          .filter(r -> !r.pattern().contains("*"))
          .forEach(routes::add);

    }
    return routes;
  }

  private static Err handle406or415(final Set<Route.Definition> routeDefs, final String method,
      final String path, final MediaType contentType, final List<MediaType> accept) {
    for (Route.Definition routeDef : routeDefs) {
      Optional<Route> route = routeDef.matches(method, path, MediaType.all, MediaType.ALL);
      if (route.isPresent() && !route.get().pattern().contains("*")) {
        if (!routeDef.canProduce(accept)) {
          return new Err(Status.NOT_ACCEPTABLE, accept.stream()
              .map(MediaType::name)
              .collect(Collectors.joining(", ")));
        }
        return new Err(Status.UNSUPPORTED_MEDIA_TYPE, contentType.name());
      }
    }
    return null;
  }

  private static String method(final String methodParam, final NativeRequest request)
      throws Exception {
    Optional<String> header = request.header(methodParam);
    if (header.isPresent()) {
      return header.get();
    }
    List<String> param = request.params(methodParam);
    return param.size() == 0 ? request.method() : param.get(0);
  }

  private static LoadingCache<RouteKey, List<Route>> routeCache(final Set<Route.Definition> routes,
      final Config conf) {
    return CacheBuilder.from(conf.getString("server.routes.Cache"))
        .build(new CacheLoader<RouteKey, List<Route>>() {
          @Override
          public List<Route> load(final RouteKey key) throws Exception {
            return routes(routes, key.method, key.path, key.consumes, key.produces);
          }
        });
  }

  private static Function<String, String> rootpath(final String applicationPath) {
    return p -> {
      if (applicationPath.equals(p)) {
        return "/";
      } else if (p.startsWith(applicationPath)) {
        return p.substring(applicationPath.length());
      } else {
        // mark as failure
        return p + '\u200B';
      }
    };
  }

}

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.Status;
import org.jooby.WebSocket;
import org.jooby.WebSocket.Definition;
import org.jooby.spi.HttpHandler;
import org.jooby.spi.NativeRequest;
import org.jooby.spi.NativeResponse;
import org.jooby.spi.NativeWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.typesafe.config.Config;

@Singleton
public class HttpHandlerImpl implements HttpHandler {

  private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

  private static final List<MediaType> ALL = ImmutableList.of(MediaType.all);

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(HttpHandler.class);

  private Set<Route.Definition> routeDefs;

  private Injector injector;

  private Set<Err.Handler> err;

  private String applicationPath;

  private RequestScope requestScope;

  private Set<Definition> socketDefs;

  private Config config;

  private int port;

  private String _method;

  @Inject
  public HttpHandlerImpl(final Injector injector,
      final RequestScope requestScope,
      final Set<Route.Definition> routes,
      final Set<WebSocket.Definition> sockets,
      final @Named("application.path") String path,
      final Set<Err.Handler> err) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.requestScope = requireNonNull(requestScope, "A request scope is required.");
    this.routeDefs = requireNonNull(routes, "Routes are required.");
    this.socketDefs = requireNonNull(sockets, "Sockets are required.");
    this.applicationPath = normalizeURI(requireNonNull(path, "An application.path is required."));
    this.err = requireNonNull(err, "An err handler is required.");
    this.config = injector.getInstance(Config.class);
    _method = this.config.getString("server.http.Method");
    this.port = config.getInt("application.port");
  }

  @Override
  public void handle(final NativeRequest request, final NativeResponse response) throws Exception {
    long start = System.currentTimeMillis();

    Map<String, Object> locals = new LinkedHashMap<>();

    Map<Object, Object> scope = new HashMap<>();

    requestScope.enter(scope);

    String verb = method(_method, request).toUpperCase();
    String requestPath = normalizeURI(request.path());
    boolean resolveAs404 = false;
    if (applicationPath.equals(requestPath)) {
      requestPath = "/";
    } else if (requestPath.startsWith(applicationPath)) {
      if (!applicationPath.equals("/")) {
        requestPath = requestPath.substring(applicationPath.length());
      }
    } else {
      resolveAs404 = true;
    }

    String contextPath = "/".equals(applicationPath) ? "" : applicationPath;
    // default locals
    locals.put("contextPath", contextPath);
    locals.put("path", requestPath);

    final String path = verb + requestPath;

    Route notFound = RouteImpl.notFound(verb, path, MediaType.ALL);

    RequestImpl req = new RequestImpl(injector, request, contextPath, port, notFound, scope,
        locals);

    ResponseImpl rsp = new ResponseImpl(injector, response, notFound, locals,
        req.charset(), request.header("Referer"));

    MediaType type = req.type();

    log.debug("handling: {}", req.path());

    log.debug("  content-type: {}", type);

    // seed req & rsp
    req.set(Request.class, req);
    req.set(Response.class, rsp);

    // seed session
    Provider<Session> session = () -> req.session();
    req.set(Session.class, session);

    boolean deferred = false;
    try {
      // not found?
      if (resolveAs404) {
        chain(ImmutableList.of(notFound)).next(req, rsp);
      }
      // websocket?
      if (socketDefs.size() > 0
          && request.header("Upgrade").orElse("").equalsIgnoreCase("WebSocket")) {
        Optional<WebSocket> sockets = findSockets(socketDefs, requestPath);
        if (sockets.isPresent()) {
          NativeWebSocket ws = request.upgrade(NativeWebSocket.class);
          ws.onConnect(() -> ((WebSocketImpl) sockets.get()).connect(injector, ws));
          return;
        }
      }

      // usual req/rsp
      List<Route> routes = routes(routeDefs, verb, requestPath, type, req.accept());

      chain(routes).next(req, rsp);

    } catch (DeferredExecution ex) {
      deferred = true;
      onDeferred(scope, request, req, rsp, ex.deferred, start);
    } catch (Exception ex) {
      handleErr(req, rsp, ex);
    } finally {
      requestScope.exit();
      if (!deferred) {
        done(req, rsp, start);
      }
    }
  }

  private void done(final RequestImpl req, final ResponseImpl rsp, final long start) {
    // mark request/response as done.
    req.done();
    rsp.end();

    long end = System.currentTimeMillis();
    log.debug("  status -> {} in {}ms", rsp.status().orElse(Status.OK), end - start);
  }

  private void onDeferred(final Map<Object, Object> scope, final NativeRequest request,
      final RequestImpl req, final ResponseImpl rsp, final Deferred deferred, final long start) {
    try {
      request.startAsync();

      deferred.handler((result, ex) -> {
        try {
          requestScope.enter(scope);
          if (result != null) {
            rsp.send(result);
          } else {
            handleErr(req, rsp, ex);
          }
        } catch (Exception exerr) {
          handleErr(req, rsp, exerr);
        } finally {
          requestScope.exit();
          done(req, rsp, start);
        }
      });
    } catch (Exception ex) {
      handleErr(req, rsp, ex);
    }
  }

  private void handleErr(final RequestImpl req, final ResponseImpl rsp, final Exception ex) {
    try {
      log.debug("execution of: " + req.method() + req.path() + " resulted in exception", ex);

      rsp.reset();

      // execution failed, find status code
      Status status = statusCode(ex);

      rsp.header("Cache-Control", NO_CACHE);
      rsp.status(status);

      Err err = ex instanceof Err ? (Err) ex : new Err(status, ex);

      Iterator<Handler> it = this.err.iterator();
      while (!rsp.committed() && it.hasNext()) {
        Err.Handler next = it.next();
        log.debug("handling err with: {}", next);
        next.handle(req, rsp, err);
      }
    } catch (Exception errex) {
      log.error("execution of err handler resulted in exception", errex);
    }
  }

  private static String normalizeURI(final String uri) {
    return uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;
  }

  private static Route.Chain chain(final List<Route> routes) {
    return new Route.Chain() {

      private int it = 0;

      @Override
      public void next(final Request req, final Response rsp) throws Exception {
        RouteImpl route = get(routes.get(it++));
        if (rsp.committed()) {
          return;
        }

        // set route
        set(req, route);
        set(rsp, route);

        route.handle(req, rsp, this);
      }

      private RouteImpl get(final Route next) {
        return (RouteImpl) Route.Forwarding.unwrap(next);
      }

      private void set(final Request req, final Route route) {
        RequestImpl root = (RequestImpl) Request.Forwarding.unwrap(req);
        root.route(route);
      }

      private void set(final Response rsp, final Route route) {
        ResponseImpl root = (ResponseImpl) Response.Forwarding.unwrap(rsp);
        root.route(route);
      }
    };
  }

  private static List<Route> routes(final Set<Route.Definition> routeDefs, final String method,
      final String path, final MediaType type, final List<MediaType> accept) {
    List<Route> routes = findRoutes(routeDefs, method, path, type, accept);

    // 406 or 415
    routes.add(RouteImpl.fromStatus((req, rsp, chain) -> {
      if (!rsp.status().isPresent()) {
        Err ex = handle406or415(routeDefs, method, path, type, accept);
        if (ex != null) {
          throw ex;
        }
      }
      chain.next(req, rsp);
    } , method, path, Status.NOT_ACCEPTABLE, accept));

    // 405
    routes.add(RouteImpl.fromStatus((req, rsp, chain) -> {
      if (!rsp.status().isPresent()) {
        Err ex = handle405(routeDefs, method, path, type, accept);
        if (ex != null) {
          throw ex;
        }
      }
      chain.next(req, rsp);
    } , method, path, Status.METHOD_NOT_ALLOWED, accept));

    // 404
    routes.add(RouteImpl.notFound(method, path, accept));

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

  private Status statusCode(final Exception ex) {
    if (ex instanceof Err) {
      return Status.valueOf(((Err) ex).statusCode());
    }
    /**
     * usually a class name, except for inner classes where '$' is replaced it by '.'
     */
    Function<Class<?>, String> name = type -> Optional.ofNullable(type.getDeclaringClass())
        .map(dc -> new StringBuilder(dc.getName())
            .append('.')
            .append(type.getSimpleName())
            .toString())
        .orElse(type.getName());

    Config err = config.getConfig("err");
    int status = -1;
    Class<?> type = ex.getClass();
    while (type != Throwable.class && status == -1) {
      String classname = name.apply(type);
      if (err.hasPath(classname)) {
        status = err.getInt(classname);
      } else {
        type = type.getSuperclass();
      }
    }
    return status == -1 ? Status.SERVER_ERROR : Status.valueOf(status);
  }

  private static Err handle405(final Set<Route.Definition> routeDefs, final String method,
      final String uri,
      final MediaType type, final List<MediaType> accept) {

    if (alternative(routeDefs, method, uri).size() > 0) {
      return new Err(Status.METHOD_NOT_ALLOWED, method + uri);
    }

    return null;
  }

  private static List<Route> alternative(final Set<Route.Definition> routeDefs, final String verb,
      final String uri) {
    List<Route> routes = new LinkedList<>();
    Set<String> verbs = Sets.newHashSet(Route.METHODS);
    verbs.remove(verb);
    for (String alt : verbs) {
      findRoutes(routeDefs, alt, uri, MediaType.all, ALL)
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
      Optional<Route> route = routeDef.matches(method, path, MediaType.all, ALL);
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

}

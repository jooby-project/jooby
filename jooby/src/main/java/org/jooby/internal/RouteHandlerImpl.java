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

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.jooby.Body;
import org.jooby.Err;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.Status;
import org.jooby.Verb;
import org.jooby.WebSocket;
import org.jooby.WebSocket.Definition;
import org.jooby.spi.Dispatcher;
import org.jooby.spi.NativeRequest;
import org.jooby.spi.NativeResponse;
import org.jooby.spi.NativeWebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;

@Singleton
public class RouteHandlerImpl implements Dispatcher {

  private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

  private static final List<MediaType> ALL = ImmutableList.of(MediaType.all);

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Set<Route.Definition> routeDefs;

  private Injector injector;

  private Err.Handler err;

  private String applicationPath;

  private RequestScope requestScope;

  private Set<Definition> socketDefs;

  @Inject
  public RouteHandlerImpl(final Injector injector,
      final RequestScope requestScope,
      final Set<Route.Definition> routes,
      final Set<WebSocket.Definition> sockets,
      final @Named("application.path") String path,
      final Err.Handler err) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.requestScope = requireNonNull(requestScope, "A request scope is required.");
    this.routeDefs = requireNonNull(routes, "Routes are required.");
    this.socketDefs = requireNonNull(sockets, "Sockets are required.");
    this.applicationPath = normalizeURI(requireNonNull(path, "An application.path is required."));
    this.err = requireNonNull(err, "An err handler is required.");
  }

  @Override
  public void handle(final NativeRequest request, final NativeResponse response) throws Exception {
    long start = System.currentTimeMillis();

    Map<Object, Object> locals = new LinkedHashMap<>();

    requestScope.enter(locals);

    Verb verb = Verb.valueOf(request.method().toUpperCase());
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

    // default locals
    locals.put("contextPath", applicationPath);
    locals.put("path", requestPath);

    final String path = verb + requestPath;

    Route notFound = RouteImpl.notFound(verb, path, MediaType.ALL);

    RequestImpl req = new RequestImpl(injector, request, notFound, locals);

    ResponseImpl rsp = new ResponseImpl(injector, response, notFound, locals, req.charset(),
        request.header("Referer"));

    MediaType type = req.type();

    log.debug("handling: {}", req.path());

    log.debug("  content-type: {}", type);

    // seed req & rsp
    req.set(Request.class, req);
    req.set(Response.class, rsp);

    // seed session
    Provider<Session> session = () -> req.session();
    req.set(Session.class, session);

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

    } catch (Exception ex) {
      log.info("execution of: " + path + " resulted in exception", ex);

      rsp.reset();

      // execution failed, so find status code
      Status status = statusCode(ex);

      rsp.header("Cache-Control", NO_CACHE);
      rsp.status(status);

      try {
        err.handle(req, rsp, ex);
      } catch (Exception ignored) {
        log.trace("execution of err handler resulted in exceptiion", ignored);
        defaultErrorPage(req, rsp, err.err(req, rsp, ex));
      }
    } finally {
      requestScope.exit();

      // mark request/response as done.
      rsp.end();

      long end = System.currentTimeMillis();
      log.debug("  status -> {} in {}ms", response.statusCode(), end - start);

      saveSession(req);
    }
  }

  private void saveSession(final Request req) {
    req.ifSession()
        .ifPresent(session -> req.require(SessionManager.class).requestDone(session));
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

  private static List<Route> routes(final Set<Route.Definition> routeDefs, final Verb verb,
      final String path, final MediaType type, final List<MediaType> accept) {
    List<Route> routes = findRoutes(routeDefs, verb, path, type, accept);

    // 406 or 415
    routes.add(RouteImpl.fromStatus((req, rsp, chain) -> {
      if (!rsp.status().isPresent()) {
        Err ex = handle406or415(routeDefs, verb, path, type, accept);
        if (ex != null) {
          throw ex;
        }
      }
      chain.next(req, rsp);
    }, verb, path, Status.NOT_ACCEPTABLE, accept));

    // 405
    routes.add(RouteImpl.fromStatus((req, rsp, chain) -> {
      if (!rsp.status().isPresent()) {
        Err ex = handle405(routeDefs, verb, path, type, accept);
        if (ex != null) {
          throw ex;
        }
      }
      chain.next(req, rsp);
    }, verb, path, Status.METHOD_NOT_ALLOWED, accept));

    // 404
    routes.add(RouteImpl.notFound(verb, path, accept));

    return routes;
  }

  private static List<Route> findRoutes(final Set<Route.Definition> routeDefs, final Verb verb,
      final String path, final MediaType type, final List<MediaType> accept) {

    List<Route> routes = new ArrayList<>();
    for (Route.Definition routeDef : routeDefs) {
      Optional<Route> route = routeDef.matches(verb, path, type, accept);
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

  private void defaultErrorPage(final RequestImpl request, final ResponseImpl rsp,
      final Map<String, Object> model) throws Exception {
    Status status = rsp.status().get();
    StringBuilder html = new StringBuilder("<!doctype html>")
        .append("<html>\n")
        .append("<head>\n")
        .append("<meta charset=\"").append(request.charset().name()).append("\">\n")
        .append("<style>\n")
        .append("body {font-family: \"open sans\",sans-serif; margin-left: 20px;}\n")
        .append("h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}\n")
        .append("h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}\n")
        .append("footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n")
        .append("hr {background-color: #f7f7f9;}\n")
        .append("div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}\n")
        .append("p {padding-left: 20px;}\n")
        .append("p.tab {padding-left: 40px;}\n")
        .append("</style>\n")
        .append("<title>\n")
        .append(status.value()).append(" ").append(status.reason())
        .append("\n</title>\n")
        .append("<body>\n")
        .append("<h1>").append(status.reason()).append("</h1>\n")
        .append("<hr>");

    model.remove("reason");

    String[] stacktrace = (String[]) model.remove("stacktrace");

    for (Entry<String, Object> entry : model.entrySet()) {
      Object value = entry.getValue();
      if (value != null) {
        html.append("<h2>").append(entry.getKey()).append(": ").append(entry.getValue())
            .append("</h2>\n");
      }
    }

    if (stacktrace != null) {
      html.append("<h2>stack:</h2>\n")
          .append("<div class=\"trace\">\n");

      Arrays.stream(stacktrace).forEach(line -> {
        html.append("<p class=\"line");
        if (line.startsWith("\t")) {
          html.append(" tab");
        }
        html.append("\">")
            .append("<code>")
            .append(line.replace("\t", "  "))
            .append("</code>")
            .append("</p>\n");
      });
      html.append("</div>\n");
    }

    html.append("<footer>powered by Jooby</footer>\n")
        .append("</body>\n")
        .append("</html>\n");

    rsp.header("Cache-Control", NO_CACHE);
    rsp.send(Body.body(html), BuiltinBodyConverter.formatAny);
  }

  private Status statusCode(final Exception ex) {
    if (ex instanceof Err) {
      return Status.valueOf(((Err) ex).statusCode());
    }
    if (ex instanceof IllegalArgumentException) {
      return Status.BAD_REQUEST;
    }
    if (ex instanceof NoSuchElementException) {
      return Status.BAD_REQUEST;
    }
    if (ex instanceof FileNotFoundException) {
      return Status.NOT_FOUND;
    }
    return Status.SERVER_ERROR;
  }

  private static Err handle405(final Set<Route.Definition> routeDefs, final Verb verb,
      final String uri,
      final MediaType type, final List<MediaType> accept) {

    if (alternative(routeDefs, verb, uri).size() > 0) {
      return new Err(Status.METHOD_NOT_ALLOWED, verb + uri);
    }

    return null;
  }

  private static List<Route> alternative(final Set<Route.Definition> routeDefs, final Verb verb,
      final String uri) {
    List<Route> routes = new LinkedList<>();
    Set<Verb> verbs = EnumSet.allOf(Verb.class);
    verbs.remove(verb);
    for (Verb alt : verbs) {
      findRoutes(routeDefs, alt, uri, MediaType.all, ALL)
          .stream()
          // skip glob pattern
          .filter(r -> !r.pattern().contains("*"))
          .forEach(routes::add);

    }
    return routes;
  }

  private static Err handle406or415(final Set<Route.Definition> routeDefs, final Verb verb,
      final String path, final MediaType contentType, final List<MediaType> accept) {
    for (Route.Definition routeDef : routeDefs) {
      Optional<Route> route = routeDef.matches(verb, path, MediaType.all, ALL);
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

}

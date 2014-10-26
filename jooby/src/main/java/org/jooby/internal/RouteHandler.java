package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jooby.MediaType;
import org.jooby.MediaTypeProvider;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Err;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;

@Singleton
public class RouteHandler {

  private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

  private static final List<MediaType> ALL = ImmutableList.of(MediaType.all);

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private BodyConverterSelector selector;

  private Set<Route.Definition> routeDefs;

  private Charset charset;

  private Locale locale;

  private Injector rootInjector;

  private Set<Request.Module> modules;

  private MediaTypeProvider typeProvider;

  private Err.Handler err;

  @Inject
  public RouteHandler(final Injector injector,
      final BodyConverterSelector selector,
      final Set<Request.Module> modules,
      final Set<Route.Definition> routes,
      final Charset defaultCharset,
      final Locale defaultLocale,
      final Route.Err.Handler err) {
    this.rootInjector = requireNonNull(injector, "An injector is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.modules = requireNonNull(modules, "Request modules are required.");
    this.routeDefs = requireNonNull(routes, "The routes are required.");
    this.charset = requireNonNull(defaultCharset, "A defaultCharset is required.");
    this.locale = requireNonNull(defaultLocale, "A defaultLocale is required.");
    this.err = requireNonNull(err, "An err handler is required.");
    this.typeProvider = injector.getInstance(MediaTypeProvider.class);
  }

  public void handle(final HttpServletRequest request, final HttpServletResponse response)
      throws Exception {
    requireNonNull(request, "A HTTP servlet request is required.");
    requireNonNull(response, "A HTTP servlet response is required.");

    long start = System.currentTimeMillis();
    Request.Verb verb = Request.Verb.valueOf(request.getMethod().toUpperCase());
    String requestURI = normalizeURI(request.getRequestURI());

    Map<String, Object> locals = new LinkedHashMap<>();

    List<MediaType> accept = Optional.ofNullable(request.getHeader("Accept"))
        .map(MediaType::parse)
        .orElse(ALL);

    if (accept.size() > 1) {
      Collections.sort(accept);
    }

    MediaType type = Optional.ofNullable(request.getHeader("Content-Type"))
        .map(MediaType::valueOf)
        .orElse(MediaType.all);

    final String path = verb + requestURI;

    log.info("handling: {}", path);

    log.debug("  content-type: {}", type);

    Charset charset = Optional.ofNullable(request.getCharacterEncoding())
        .map(Charset::forName)
        .orElse(this.charset);

    Locale locale = Optional.ofNullable(request.getHeader("Accept-Language"))
        .map(l -> request.getLocale()).orElse(this.locale);

    BiFunction<Injector, Route, Request> reqFactory = (injector, route) ->
        new RequestImpl(request, injector, route, selector, type, accept, charset, locale);

    BiFunction<Injector, Route, Response> resFactory = (injector, route) ->
        new ResponseImpl(response, injector, route, locals, selector, typeProvider, charset,
            Optional.ofNullable(request.getHeader("Referer")));

    Injector injector = rootInjector;

    Route notFound = RouteImpl.notFound(verb, path, accept);

    try {

      // configure request modules
      injector = rootInjector.createChildInjector(binder -> {
        for (Request.Module module : modules) {
          module.configure(binder);
        }
      });

      List<Route> routes = routes(verb, requestURI, type, accept);

      chain(routes.iterator())
          .next(reqFactory.apply(injector, notFound), resFactory.apply(injector, notFound));

    } catch (Exception ex) {
      log.debug("execution of: " + path + " resulted in exception", ex);

      // reset response
      response.reset();

      Request req = reqFactory.apply(injector, notFound);
      Response res = resFactory.apply(injector, notFound);

      // execution failed, so find status code
      Response.Status status = statusCode(ex);

      res.header("Cache-Control", NO_CACHE);
      res.status(status);

      try {
        err.handle(req, res, ex);
      } catch (Exception ignored) {
        log.debug("rendering of error failed, fallback to default error page", ignored);
        defaultErrorPage(req, res, err.err(req, res, ex));
      }
    } finally {
      long end = System.currentTimeMillis();
      log.info("  status -> {} in {}ms", response.getStatus(), end - start);
    }
  }

  public Injector injector() {
    return rootInjector;
  }

  private static String normalizeURI(final String uri) {
    return uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;
  }

  private static Route.Chain chain(final Iterator<Route> it) {
    return new Route.Chain() {

      @Override
      public void next(final Request req, final Response res) throws Exception {
        RouteImpl route = get(it.next());

        // set route
        set(req, route);
        set(res, route);

        route.handle(req, res, this);
      }

      private RouteImpl get(final Route next) {
        return (RouteImpl) Route.Forwarding.unwrap(next);
      }

      private void set(final Request req, final Route route) {
        RequestImpl root = (RequestImpl) Request.Forwarding.unwrap(req);
        root.route(route);
      }

      private void set(final Response res, final Route route) {
        ResponseImpl root = (ResponseImpl) Response.Forwarding.unwrap(res);
        root.route(route);
      }
    };
  }

  private List<Route> routes(final Request.Verb verb, final String path, final MediaType type,
      final List<MediaType> accept) {
    List<Route> routes = findRoutes(verb, path, type, accept);

    // 406 or 415
    routes.add(RouteImpl.fromStatus((req, res, chain) -> {
      if (!res.committed()) {
        Route.Err ex = handle406or415(verb, path, type, accept);
        if (ex != null) {
          throw ex;
        }
      }
      chain.next(req, res);
    }, verb, path, Response.Status.NOT_ACCEPTABLE, accept));

    // 405
    routes.add(RouteImpl.fromStatus((req, res, chain) -> {
      if (!res.committed()) {
        Route.Err ex = handle405(verb, path, type, accept);
        if (ex != null) {
          throw ex;
        }
      }
      chain.next(req, res);
    }, verb, path, Response.Status.METHOD_NOT_ALLOWED, accept));

    // 404
    routes.add(RouteImpl.notFound(verb, path, accept));

    return routes;
  }

  private List<Route> findRoutes(final Request.Verb verb, final String path, final MediaType type,
      final List<MediaType> accept) {
    return findRoutes(verb, path, type, accept, verb);
  }

  private List<Route> findRoutes(final Request.Verb verb, final String path, final MediaType type,
      final List<MediaType> accept, final Request.Verb overrideVerb) {

    LinkedList<Route> routes = new LinkedList<Route>();
    for (Route.Definition routeDef : routeDefs) {
      Optional<Route> route = routeDef.matches(verb, path, type, accept);
      if (route.isPresent()) {
        routes.add(verb.equals(overrideVerb) ? route.get()
            : overrideVerb(route.get(), overrideVerb));
      }
    }
    return routes;
  }

  private static Route overrideVerb(final Route route, final Request.Verb verb) {
    return new Route.Forwarding(route) {
      @Override
      public Request.Verb verb() {
        return verb;
      }
    };
  }

  private void defaultErrorPage(final Request request, final Response res,
      final Map<String, Object> model) throws Exception {
    Response.Status status = res.status().get();
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
            .append(line)
            .append("</code>")
            .append("</p>\n");
      });
      html.append("</div>\n");
    }

    html.append("<footer>powered by Jooby</footer>\n")
        .append("</body>\n")
        .append("</html>\n");

    res.header("Cache-Control", NO_CACHE);
    res.send(html, FallbackBodyConverter.TO_HTML);
  }

  private Response.Status statusCode(final Exception ex) {
    if (ex instanceof Route.Err) {
      return ((Route.Err) ex).status();
    }
    if (ex instanceof IllegalArgumentException) {
      return Response.Status.BAD_REQUEST;
    }
    if (ex instanceof NoSuchElementException) {
      return Response.Status.BAD_REQUEST;
    }
    return Response.Status.SERVER_ERROR;
  }

  private Route.Err handle405(final Request.Verb verb, final String uri, final MediaType type,
      final List<MediaType> accept) {

    if (alternative(verb, uri).size() > 0) {
      return new Route.Err(Response.Status.METHOD_NOT_ALLOWED, verb + uri);
    }

    return null;
  }

  private List<Route> alternative(final Request.Verb verb, final String uri) {
    List<Route> routes = new LinkedList<>();
    Set<Request.Verb> verbs = EnumSet.allOf(Request.Verb.class);
    verbs.remove(verb);
    for (Request.Verb alt : verbs) {
      findRoutes(alt, uri, MediaType.all, ALL)
          .stream()
          // skip glob pattern
          .filter(r -> !r.pattern().contains("*"))
          .forEach(routes::add);

    }
    return routes;
  }

  private Route.Err handle406or415(final Request.Verb verb, final String path,
      final MediaType contentType, final List<MediaType> accept) {
    for (Route.Definition routeDef : routeDefs) {
      Optional<Route> route = routeDef.matches(verb, path, MediaType.all, ALL);
      if (route.isPresent() && !route.get().pattern().contains("*")) {
        if (!routeDef.canProduce(accept)) {
          return new Route.Err(Response.Status.NOT_ACCEPTABLE, accept.stream()
              .map(MediaType::name)
              .collect(Collectors.joining(", ")));
        }
        return new Route.Err(Response.Status.UNSUPPORTED_MEDIA_TYPE, contentType.name());
      }
    }
    return null;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    int verbMax = 0, routeMax = 0, consumesMax = 0, producesMax = 0;
    for (Route.Definition routeDef : routeDefs) {
      verbMax = Math.max(verbMax, routeDef.verb().length());

      routeMax = Math.max(routeMax, routeDef.pattern().length());

      consumesMax = Math.max(consumesMax, routeDef.consumes().toString().length());

      producesMax = Math.max(producesMax, routeDef.produces().toString().length());
    }

    String format = "  %-" + verbMax + "s %-" + routeMax + "s    %" + consumesMax + "s     %"
        + producesMax + "s    (%s)\n";

    for (Route.Definition routeDef : routeDefs) {
      buffer.append(String.format(format, routeDef.verb(), routeDef.pattern(),
          routeDef.consumes(), routeDef.produces(), routeDef.name()));
    }

    return buffer.toString();
  }
}

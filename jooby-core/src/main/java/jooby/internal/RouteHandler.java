package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
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

import jooby.FileMediaTypeProvider;
import jooby.ForwardingRequest;
import jooby.ForwardingResponse;
import jooby.HttpException;
import jooby.HttpStatus;
import jooby.MediaType;
import jooby.Request;
import jooby.RequestModule;
import jooby.Response;
import jooby.Route;
import jooby.RouteDefinition;
import jooby.Viewable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Injector;

@Singleton
public class RouteHandler {

  private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

  private static final List<String> VERBS = ImmutableList.of("GET", "POST", "PUT", "DELETE");

  private static final List<MediaType> ALL = ImmutableList.of(MediaType.all);

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private BodyConverterSelector selector;

  private Set<RouteDefinition> routeDefs;

  private Charset charset;

  private Injector rootInjector;

  private Set<RequestModule> modules;

  private FileMediaTypeProvider typeProvider;

  @Inject
  public RouteHandler(final Injector injector,
      final BodyConverterSelector selector,
      final Set<RequestModule> modules,
      final Set<RouteDefinition> routes,
      final Charset defaultCharset) {
    this.rootInjector = requireNonNull(injector, "An injector is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.modules = requireNonNull(modules, "Request modules are required.");
    this.routeDefs = requireNonNull(routes, "The routes are required.");
    this.charset = requireNonNull(defaultCharset, "A defaultCharset is required.");
    this.typeProvider = injector.getInstance(FileMediaTypeProvider.class);
  }

  public void handle(final HttpServletRequest request, final HttpServletResponse response)
      throws Exception {
    requireNonNull(request, "A HTTP servlet request is required.");
    requireNonNull(response, "A HTTP servlet response is required.");

    long start = System.currentTimeMillis();
    String verb = request.getMethod().toUpperCase();
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

    log.info("  content-type: {}", type);

    Charset charset = Optional.ofNullable(request.getCharacterEncoding())
        .map(Charset::forName)
        .orElse(this.charset);

    BiFunction<Injector, Route, Request> reqFactory = (injector, route) ->
        new RequestImpl(request, injector, route, selector, charset, type, accept);

    BiFunction<Injector, Route, Response> resFactory = (injector, route) ->
        new ResponseImpl(response, injector, route, locals, selector, typeProvider, charset);

    Injector injector = rootInjector;

    Route notFound = RouteImpl.notFound(verb, path, accept);

    try {

      // configure request modules
      injector = rootInjector.createChildInjector(binder -> {
        for (RequestModule module : modules) {
          module.configure(binder);
        }
      });

      List<Route> routes = routes(verb, requestURI, type, accept);

      chain(routes.iterator())
          .next(reqFactory.apply(injector, notFound), resFactory.apply(injector, notFound));

    } catch (Exception ex) {
      log.error("handling of: " + path + " ends with error", ex);
      // reset response
      response.reset();

      Request req = reqFactory.apply(injector, notFound);
      Response res = resFactory.apply(injector, notFound);

      // execution failed, so find status code
      HttpStatus status = statusCode(ex);

      res.header("Cache-Control", NO_CACHE);
      res.status(status);

      // TODO: move me to an error handler feature
      Map<String, Object> model = errorModel(req, ex, status);
      try {
        res.format()
            .when(MediaType.html, () -> Viewable.of("/status/" + status.value(), model))
            .when(MediaType.all, () -> model)
            .send();
      } catch (Exception ignored) {
        log.trace("rendering of error failed, fallback to default error page", ignored);
        defaultErrorPage(req, res, status, model);
      }
    } finally {
      long end = System.currentTimeMillis();
      log.info("  status -> {} in {}ms", response.getStatus(), end - start);
    }
  }

  private static String normalizeURI(final String uri) {
    return uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri;
  }

  private static Route.Chain chain(final Iterator<Route> it) {
    return new Route.Chain() {

      @Override
      public void next(final Request req, final Response res) throws Exception {
        RouteImpl route = (RouteImpl) it.next();

        // set route
        set(req, route);
        set(res, route);

        route.handle(req, res, this);
      }

      private void set(final Request req, final Route route) {
        Request root = req;
        // Is there a better way to set route info?
        while (root instanceof ForwardingRequest) {
          root = ((ForwardingRequest) root).delegate();
        }
        if (root instanceof RequestImpl) {
          ((RequestImpl) root).route(route);
        }
      }

      private void set(final Response res, final Route route) {
        Response root = res;
        // Is there a better way to set route info?
        while (root instanceof ForwardingResponse) {
          root = ((ForwardingResponse) root).delegate();
        }
        if (root instanceof ResponseImpl) {
          ((ResponseImpl) root).route(route);
        }
      }
    };
  }

  private List<Route> routes(final String verb, final String requestURI, final MediaType type,
      final List<MediaType> accept) {
    String path = verb + requestURI;
    List<Route> routes = findRoutes(verb, requestURI, type, accept);

    // 406 or 415
    routes.add(RouteImpl.fromStatus((req, res, chain) -> {
      if (!res.committed()) {
        HttpException ex = handle406or415(verb, requestURI, type, accept);
        if (ex != null) {
          throw ex;
        }
      }
      chain.next(req, res);
    }, verb, path, HttpStatus.NOT_ACCEPTABLE, accept));

    // 405
    routes.add(RouteImpl.fromStatus((req, res, chain) -> {
      if (!res.committed()) {
        HttpException ex = handle405(verb, requestURI, type, accept);
        if (ex != null) {
          throw ex;
        }
      }
      chain.next(req, res);
    }, verb, path, HttpStatus.METHOD_NOT_ALLOWED, accept));

    // 404
    routes.add(RouteImpl.notFound(verb, path, accept));

    return routes;
  }

  private List<Route> findRoutes(final String verb, final String path, final MediaType type,
      final List<MediaType> accept) {

    LinkedList<Route> routes = new LinkedList<Route>();
    for (RouteDefinition routeDef : routeDefs) {
      Optional<Route> route = routeDef.matches(verb, path, type, accept);
      if (route.isPresent()) {
        routes.add(route.get());
      }
    }
    return routes;
  }

  private void defaultErrorPage(final Request request, final Response response,
      final HttpStatus status, final Map<String, Object> model) throws Exception {
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

    String[] stacktrace = (String[]) model.remove("stackTrace");

    for (Entry<String, Object> entry : model.entrySet()) {
      Object value = entry.getValue();
      if (value != null) {
        html.append("<h2>").append(entry.getKey()).append(": ").append(entry.getValue())
            .append("</h2>\n");
      }
    }

    if (stacktrace != null) {
      html.append("<h2>stackTrace:</h2>\n")
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

    response.header("Cache-Control", NO_CACHE);
    response.send(html, FallbackBodyConverter.TO_HTML);
  }

  private Map<String, Object> errorModel(final Request request, final Exception ex,
      final HttpStatus status) {
    Map<String, Object> error = new LinkedHashMap<>();
    String message = ex.getMessage();
    message = message == null ? status.reason() : message;
    error.put("message", message);
    error.put("stackTrace", dump(ex));
    error.put("status", status.value());
    error.put("reason", status.reason());
    return error;
  }

  private static String[] dump(final Exception ex) {
    StringWriter writer = new StringWriter();
    ex.printStackTrace(new PrintWriter(writer));
    String[] stacktrace = writer.toString().replace("\r", "").split("\\n");
    return stacktrace;
  }

  private HttpStatus statusCode(final Exception ex) {
    if (ex instanceof HttpException) {
      return ((HttpException) ex).status();
    }
    // Class<?> type = ex.getClass();
    // Status status = errorMap.get(type);
    // while (status == null && type != Throwable.class) {
    // type = type.getSuperclass();
    // status = errorMap.get(type);
    // }
    // return status == null ? defaultStatusCode(ex) : status;
    // TODO: finished me
    return defaultStatusCode(ex);
  }

  private HttpStatus defaultStatusCode(final Exception ex) {
    if (ex instanceof IllegalArgumentException) {
      return HttpStatus.BAD_REQUEST;
    }
    if (ex instanceof NoSuchElementException) {
      return HttpStatus.BAD_REQUEST;
    }
    return HttpStatus.SERVER_ERROR;
  }

  private HttpException handle405(final String verb, final String uri, final MediaType type,
      final List<MediaType> accept) {

    List<String> verbs = Lists.newLinkedList(VERBS);
    verbs.remove(verb);
    for (String candidate : verbs) {
      Optional<Route> found = findRoutes(candidate, uri, type, accept)
          .stream()
          // skip glob pattern
          .filter(r -> !r.pattern().contains("*"))
          .findFirst();

      if (found.isPresent()) {
        return new HttpException(HttpStatus.METHOD_NOT_ALLOWED, verb + uri);
      }
    }
    return null;
  }

  private HttpException handle406or415(final String verb, final String path,
      final MediaType contentType, final List<MediaType> accept) {
    for (RouteDefinition routeDef : routeDefs) {
      Optional<Route> route = routeDef.matches(verb, path, MediaType.all, ALL);
      if (route.isPresent() && !route.get().pattern().contains("*")) {
        if (!routeDef.canProduce(accept)) {
          return new HttpException(HttpStatus.NOT_ACCEPTABLE, accept.stream()
              .map(MediaType::name)
              .collect(Collectors.joining(", ")));
        }
        return new HttpException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, contentType.name());
      }
    }
    return null;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    int verbMax = 0, routeMax = 0, consumesMax = 0, producesMax = 0;
    for (RouteDefinition routeDef : routeDefs) {
      verbMax = Math.max(verbMax, routeDef.verb().length());

      routeMax = Math.max(routeMax, routeDef.pattern().length());

      consumesMax = Math.max(consumesMax, routeDef.consumes().toString().length());

      producesMax = Math.max(producesMax, routeDef.produces().toString().length());
    }

    String format = "  %-" + verbMax + "s %-" + routeMax + "s    %" + consumesMax + "s     %"
        + producesMax + "s    (%s)\n";

    for (RouteDefinition routeDef : routeDefs) {
      buffer.append(String.format(format, routeDef.verb(), routeDef.pattern(),
          routeDef.consumes(), routeDef.produces(), routeDef.name()));
    }
    return buffer.toString();
  }
}

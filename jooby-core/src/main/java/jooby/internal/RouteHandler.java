package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Iterator;
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
import javax.inject.Singleton;

import jooby.HttpException;
import jooby.HttpStatus;
import jooby.MediaType;
import jooby.Request;
import jooby.RequestModule;
import jooby.Response;
import jooby.RouteChain;
import jooby.RouteDefinition;
import jooby.RouteMatcher;
import jooby.RoutePattern;
import jooby.Viewable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.inject.Injector;

@Singleton
public class RouteHandler {

  private static class RouteDescriptor {

    public final RouteDefinition definition;

    public final RouteMatcher matcher;

    public final List<MediaType> produces;

    public RouteDescriptor(final RouteDefinition definition, final RouteMatcher matcher,
        final List<MediaType> produces) {
      this.definition = definition;
      this.matcher = matcher;
      this.produces = produces;
    }

    @Override
    public String toString() {
      return matcher.toString();
    }
  }

  private static final String NO_CACHE = "must-revalidate,no-cache,no-store";

  private static final List<String> VERBS = ImmutableList.of("GET", "POST", "PUT", "DELETE");

  private static final List<MediaType> ALL = ImmutableList.of(MediaType.all);

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private BodyConverterSelector selector;

  private Set<RouteDefinition> routes;

  private Charset charset;

  private Injector rootInjector;

  private Set<RequestModule> requestModules;

  @Inject
  public RouteHandler(final Injector injector,
      final BodyConverterSelector selector,
      final Set<RequestModule> requestModules,
      final Set<RouteDefinition> routes,
      final Charset defaultCharset) {
    this.rootInjector = requireNonNull(injector, "An injector is required.");
    this.selector = requireNonNull(selector, "A message converter selector is required.");
    this.requestModules = requireNonNull(requestModules, "Request modules are required.");
    this.routes = requireNonNull(routes, "The routes are required.");
    this.charset = requireNonNull(defaultCharset, "A defaultCharset is required.");
  }

  public void handle(final String verb, final String uri,
      final String contentType,
      final String accept,
      final String charset,
      final Map<String, String[]> parameters,
      final ListMultimap<String, String> responseHeaders,
      final RequestFactory reqFactory,
      final ResponseFactory respFactory) throws Exception {
    requireNonNull(verb, "A HTTP verb is required.");
    requireNonNull(uri, "A Request URI is required.");
    requireNonNull(parameters, "The request parameters are required.");
    requireNonNull(responseHeaders, "The response headers are required.");
    requireNonNull(reqFactory, "A request factory is required.");
    requireNonNull(respFactory, "A response factory is required.");

    final List<MediaType> acceptList = Optional.ofNullable(accept).map(MediaType::parse)
        .orElse(ALL);

    MediaType type = Optional.ofNullable(contentType).map(MediaType::valueOf).orElse(MediaType.all);

    doHandle(verb.toUpperCase(),
        uri.endsWith("/") && uri.length() > 1 ? uri.substring(0, uri.length() - 1) : uri,
        type,
        acceptList,
        Optional.ofNullable(charset).map(Charset::forName).orElse(this.charset),
        parameters,
        responseHeaders,
        reqFactory,
        respFactory);
  }

  private void doHandle(final String verb, final String requestURI,
      final MediaType contentType,
      final List<MediaType> accept,
      final Charset charset,
      final Map<String, String[]> params,
      final ListMultimap<String, String> responseHeaders,
      final RequestFactory reqFactory,
      final ResponseFactory respFactory) throws Exception {

    long start = System.currentTimeMillis();
    final String path = verb + requestURI;

    log.info("handling: {}", path);

    log.info("  content-type: {}", contentType);

    // configure request modules
    Injector injector = rootInjector.createChildInjector(binder -> {
      for (RequestModule requestModule : requestModules) {
        requestModule.configure(binder);
      }
    });

    final List<RouteDescriptor> descriptors = routes(routes, verb, requestURI, contentType,
        accept);

    final List<MediaType> produces = descriptors.size() > 0 ? descriptors.get(0).produces : accept;
    log.trace("  produces: {}", produces);

    final Request request = reqFactory.newRequest(injector,
        descriptors.size() > 0 ? descriptors.get(0).matcher : noMatch(requestURI),
        selector,
        charset,
        contentType,
        accept);

    final Response response = respFactory.newResponse(selector,
        request.charset(),
        produces,
        responseHeaders);

    try {

      chain(descriptors.iterator()).next(request, response);

    } catch (Exception ex) {
      log.error("handling of: " + path + " ends with error", ex);
      ((ResponseImpl) response).reset();
      // execution failed, so find status code
      HttpStatus status = statusCode(ex);

      response.header("Cache-Control").setString(NO_CACHE);
      response.status(status);

      // TODO: move me to an error handler feature
      Map<String, Object> model = errorModel(request, ex, status);
      try {
        response
            .when(MediaType.html, () -> Viewable.of("/status/" + status.value(), model))
            .when(MediaType.all, () -> model)
            .send();
      } catch (Exception ignored) {
        log.trace("rendering of error failed, fallback to default error page", ignored);
        defaultErrorPage(request, response, status, model);
      }
    } finally {
      long end = System.currentTimeMillis();
      log.info("  status -> {} in {}ms", response.status(), end
          - start);
      ((RequestImpl) request).destroy();
    }
  }

  private static RouteChain chain(final Iterator<RouteDescriptor> it) {
    return new RouteChain() {

      private LinkedList<String> stack = new LinkedList<>();

      @Override
      public void next(final Request request, final Response response) throws Exception {
        RouteDescriptor desc = it.next();
        stack.addLast(desc.matcher.toString());

        RouteDefinition next = desc.definition;

        next.handle(request, response, this);

        stack.removeLast();
      }

      @Override
      public String toString() {
        StringBuilder buff = new StringBuilder();
        String pad = "  ";
        for (String router : stack) {
          buff.append(router).append("\n").append(pad);
          pad += pad;
        }
        return buff.toString();
      }
    };
  }

  private static RouteMatcher noMatch(final String path) {
    return new RouteMatcher() {

      @Override
      public String path() {
        return path;
      }

      @Override
      public boolean matches() {
        return false;
      }

      @Override
      public RoutePattern pattern() {
        return null;
      }
    };
  }

  private static List<RouteDescriptor> routes(final Set<RouteDefinition> routes,
      final String verb,
      final String requestURI,
      final MediaType contentType,
      final List<MediaType> accept) {
    String path = verb + requestURI;
    List<RouteDescriptor> routers = findRoutes(routes, path, contentType, accept);

    // 406 or 415
    routers.add(new RouteDescriptor(new RouteDefinitionImpl(verb, path, (req, resp, chain) -> {
      if (!resp.committed()) {
        HttpException ex = handle406or415(routes, verb, requestURI, contentType, accept);
        if (ex != null) {
          throw ex;
        }
      }
      chain.next(req, resp);
    }), noMatch(path), accept));

    // 405
    routers.add(new RouteDescriptor(new RouteDefinitionImpl(verb, path, (req, resp, chain) -> {
      if (!resp.committed()) {
        HttpException ex = handle405(routes, verb, requestURI, contentType, accept);
        if (ex != null) {
          throw ex;
        }
      }
      chain.next(req, resp);
    }), noMatch(path), accept));

    // 404
    routers.add(new RouteDescriptor(new RouteDefinitionImpl(verb, path, (req, resp, chain) -> {
      if (!resp.committed()) {
        throw new HttpException(HttpStatus.NOT_FOUND, path);
      }
    }), noMatch(path), accept));
    return routers;
  }

  private static List<RouteDescriptor> findRoutes(final Set<RouteDefinition> routes,
      final String path,
      final MediaType contentType,
      final List<MediaType> accept) {
    LinkedList<RouteDescriptor> routers = new LinkedList<RouteDescriptor>();
    for (RouteDefinition route : routes) {
      RouteMatcher matcher = route.matcher(path);
      if (matcher.matches()) {
        List<MediaType> produces = MediaType.matcher(accept).filter(route.produces());
        if (route.canConsume(contentType) && produces.size() > 0) {
          routers.add(new RouteDescriptor(route, matcher, produces));
        }
      }
    }
    return routers;
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

    response.header("Cache-Control").setString(NO_CACHE);
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

  private static HttpException handle405(final Set<RouteDefinition> routes,
      final String verb, final String requestURI,
      final MediaType contentType,
      final List<MediaType> accept) {

    List<String> verbs = Lists.newLinkedList(VERBS);
    verbs.remove(verb);
    for (String candidate : verbs) {
      String path = candidate + requestURI;
      Optional<RouteDescriptor> found = findRoutes(routes, path, contentType, accept).stream()
          .filter(desc -> !desc.matcher.pattern().regex()).findFirst();
      if (found.isPresent()) {
        return new HttpException(HttpStatus.METHOD_NOT_ALLOWED, verb + requestURI);
      }
    }
    return null;
  }

  private static HttpException handle406or415(final Set<RouteDefinition> routes,
      final String verb, final String requestURI,
      final MediaType contentType,
      final List<MediaType> accept) {
    String path = verb + requestURI;
    for (RouteDefinition route : routes) {
      RouteMatcher matcher = route.matcher(path);
      if (matcher.matches() && !route.path().regex()) {
        if (!route.canProduce(accept)) {
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
    int routeMax = 0, consumesMax = 0, producesMax = 0;
    for (RouteDefinition routeDefinition : routes) {
      int routeLen = routeDefinition.path().toString().length();
      if (routeLen > routeMax) {
        routeMax = routeLen;
      }
      //
      int consumeLen = routeDefinition.consumes().toString().length();
      if (consumeLen > consumesMax) {
        consumesMax = consumeLen;
      }
      int producesLen = routeDefinition.produces().toString().length();
      if (producesLen > producesMax) {
        producesMax = producesLen;
      }
    }
    String format = "    %-" + routeMax + "s    %" + consumesMax + "s     %" + producesMax + "s\n";
    for (RouteDefinition routeDefinition : routes) {
      buffer.append(String.format(format, routeDefinition.path(), routeDefinition.consumes(),
          routeDefinition.produces()));
    }
    return buffer.toString();
  }
}

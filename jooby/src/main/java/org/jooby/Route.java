package org.jooby;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.jooby.Request.Verb;
import org.jooby.internal.RouteImpl;
import org.jooby.internal.RouteMatcher;
import org.jooby.internal.RoutePattern;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.Beta;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Routes are a key concept in Jooby. Routes are executed in the same order they are defined
 * (even for Mvc Routes).
 *
 * <h1>Handlers</h1> There are two types of handlers: {@link Router} and {@link Filter}. They behave
 * very similar,
 * except that a {@link Filter} can decide if the next route handler can be executed or not. For
 * example:
 *
 * <pre>
 *   get("/filter", (req, res, chain) -> {
 *     if (someCondition) {
 *       chain.next(req, res);
 *     } else {
 *       // respond, throw err, etc...
 *     }
 *   });
 * </pre>
 *
 * A router always execute the next handler:
 *
 * <pre>
 *   get("router", (req, res) -> {
 *     res.send("router");
 *   });
 *
 *   // filter version
 *   get("router", (req, res, chain) -> {
 *     res.send("router");
 *     chain.next(req, res);
 *   });
 * </pre>
 *
 * Both handlers are identical.
 *
 * <h1>Path Patterns</h1>
 * <p>
 * Jooby supports Ant-style path patterns:
 * </p>
 * <p>
 * Some examples:
 * </p>
 * <ul>
 * <li>{@code com/t?st.html} - matches {@code com/test.html} but also {@code com/tast.jsp} or
 * {@code com/txst.html}</li>
 * <li>{@code com/*.html} - matches all {@code .html} files in the {@code com} directory</li>
 * <li><code>com/{@literal **}/test.html</code> - matches all {@code test.html} files underneath the
 * {@code com} path</li>
 * <li>{@code **}/{@code *} - matches any path at any level.</li>
 * <li>{@code *} - matches any path at any level, shorthand for {@code {@literal **}/{@literal *}.</li>
 * </ul>
 *
 * <h2>Variables</h2>
 * <p>
 * Jooby supports path parameters too:
 * </p>
 * <p>
 * Some examples:
 * </p>
 * <ul>
 * <li><code> /user/{id}</code> - /user/* and give you access to the <code>id</code> var.</li>
 * <li><code> /user/:id</code> - /user/* and give you access to the <code>id</code> var.</li>
 * <li><code> /user/{id:\\d+}</code> - /user/[digits] and give you access to the numeric
 * <code>id</code> var.</li>
 * </ul>
 *
 * <h1>Routes</h1>
 * <p>
 * Routes perform actions in response to a server HTTP request. There are two types of routes
 * callback: {@link Router} and {@link Filter}.
 * </p>
 * <p>
 * Routes are executed in the order they are defined, for example:
 *
 * <pre>
 *   get("/", (req, res) -> {
 *     log.info("first"); // start here and go to second
 *   });
 *
 *   get("/", (req, res) -> {
 *     log.info("second"); // execute after first and go to final
 *   });
 *
 *   get("/", (req, res) -> {
 *     res.send("final"); // done!
 *   });
 * </pre>
 *
 * Please note first and second routes are converted to a filter, so previous example is the same
 * as:
 *
 * <pre>
 *   get("/", (req, res, chain) -> {
 *     log.info("first"); // start here and go to second
 *     chain.next(req, res);
 *   });
 *
 *   get("/", (req, res, chain) -> {
 *     log.info("second"); // execute after first and go to final
 *     chain.next(req, res);
 *   });
 *
 *   get("/", (req, res) -> {
 *     res.send("final"); // done!
 *   });
 * </pre>
 *
 * </p>
 *
 * <h2>Inline route</h2>
 * <p>
 * An inline route can be defined using Lambda expressions, like:
 * </p>
 *
 * <pre>
 *   get("/", (request, response) -> {
 *     response.send("Hello Jooby");
 *   });
 * </pre>
 *
 * Due to the use of lambdas a route is a singleton and you should NOT use global variables.
 * For example this is a bad practice:
 *
 * <pre>
 *  List<String> names = new ArrayList<>(); // names produces side effects
 *  get("/", (req, res) -> {
 *     names.add(req.param("name").stringValue();
 *     // response will be different between calls.
 *     res.send(names);
 *   });
 * </pre>
 *
 * <h2>External route</h2>
 * <p>
 * An external route can be defined by using a {@link Class route class}, like:
 * </p>
 *
 * <pre>
 *   get("/", route(ExternalRoute.class)); //or
 *
 *   ...
 *   // ExternalRoute.java
 *   public class ExternalRoute implements Router {
 *     public void handle(Request req, Response res) throws Exception {
 *       res.send("Hello Jooby");
 *     }
 *   }
 * </pre>
 *
 * <h2>Mvc Route</h2>
 * <p>
 * A Mvc Route use annotations to define routes:
 * </p>
 *
 * <pre>
 *   route(MyRoute.class);
 *   ...
 *   // MyRoute.java
 *   {@literal @}Path("/")
 *   public class MyRoute {
 *
 *    {@literal @}GET
 *    public String hello() {
 *      return "Hello Jooby";
 *    }
 *   }
 * </pre>
 * <p>
 * Programming model is quite similar to JAX-RS/Jersey with some minor differences and/or
 * simplifications.
 * </p>
 *
 * <p>
 * To learn more about Mvc Routes, please check {@link org.jooby.mvc.Path},
 * {@link org.jooby.mvc.Produces} {@link org.jooby.mvc.Consumes}, {@link org.jooby.mvc.Body} and
 * {@link org.jooby.mvc.Template}.
 * </p>
 *
 * @author edgar
 * @sine 0.1.0
 */
@Beta
public interface Route {

  /**
   * DSL for customize routes.
   *
   * <h1>Defining a new route</h1>
   * <p>
   * It's pretty straight forward:
   * </p>
   *
   * <pre>
   *   public class MyApp extends Jooby {
   *     {
   *        get("/", (req, res) -> res.send("GET"));
   *
   *        post("/", (req, res) -> res.send("POST"));
   *
   *        put("/", (req, res) -> res.send("PUT"));
   *
   *        delete("/", (req, res) -> res.status(Response.Status.NO_CONTENT));
   *     }
   *   }
   * </pre>
   *
   * <h1>Setting what a route can consumes</h1>
   *
   * <pre>
   *   public class MyApp extends Jooby {
   *     {
   *        post("/", (req, resp) -> resp.send("POST"))
   *          .consumes(MediaType.json);
   *     }
   *   }
   * </pre>
   *
   * <h1>Setting what a route can produces</h1>
   *
   * <pre>
   *   public class MyApp extends Jooby {
   *     {
   *        post("/", (req, resp) -> resp.send("POST"))
   *          .produces(MediaType.json);
   *     }
   *   }
   * </pre>
   *
   * <h1>Adding a name</h1>
   *
   * <pre>
   *   public class MyApp extends Jooby {
   *     {
   *        post("/", (req, resp) -> resp.send("POST"))
   *          .name("My Root");
   *     }
   *   }
   * </pre>
   *
   * @author edgar
   * @since 0.1.0
   */
  @Beta
  class Definition {

    /**
     * Route's name.
     */
    private String name = "anonymous";

    /**
     * A route pattern.
     */
    private RoutePattern compiledPattern;

    /**
     * The target route.
     */
    private Filter filter;

    /**
     * Defines the media types that the methods of a resource class or can accept. Default is:
     * {@literal *}/{@literal *}.
     */
    private List<MediaType> consumes = MediaType.ALL;

    /**
     * Defines the media types that the methods of a resource class or can produces. Default is:
     * {@literal *}/{@literal *}.
     */
    private List<MediaType> produces = MediaType.ALL;

    /**
     * A HTTP verb or <code>*</code>.
     */
    private String verb;

    /**
     * A path pattern.
     */
    private String pattern;

    /**
     * Creates a new route definition.
     *
     * @param verb A HTTP verb or <code>*</code>.
     * @param pattern A path pattern.
     * @param router A route handler.
     */
    public Definition(final @Nonnull String verb, final @Nonnull String pattern,
        final @Nonnull Router router) {
      this(verb, pattern, (req, res, chain) -> {
        router.handle(req, res);
        chain.next(req, res);
      });
    }

    /**
     * Creates a new route definition.
     *
     * @param verb A HTTP verb or <code>*</code>.
     * @param pattern A path pattern.
     * @param router A route handler.
     */
    public Definition(final @Nonnull String verb, final @Nonnull String pattern,
        final @Nonnull Filter filter) {
      try {
        Request.Verb.valueOf(verb.toUpperCase());
      } catch (NullPointerException | IllegalArgumentException ex) {
        if (!"*".equals(verb)) {
          throw new IllegalArgumentException("Invalid HTTP verb: " + verb);
        }
      }
      requireNonNull(pattern, "A route path is required.");
      requireNonNull(filter, "A filter is required.");

      this.verb = verb.toUpperCase();
      this.compiledPattern = new RoutePattern(verb, pattern);
      // normalized pattern
      this.pattern = compiledPattern.pattern();
      this.filter = filter;
    }

    /**
     * <h1>Path Patterns</h1>
     * <p>
     * Jooby supports Ant-style path patterns:
     * </p>
     * <p>
     * Some examples:
     * </p>
     * <ul>
     * <li>{@code com/t?st.html} - matches {@code com/test.html} but also {@code com/tast.jsp} or
     * {@code com/txst.html}</li>
     * <li>{@code com/*.html} - matches all {@code .html} files in the {@code com} directory</li>
     * <li><code>com/{@literal **}/test.html</code> - matches all {@code test.html} files underneath
     * the {@code com} path</li>
     * <li>{@code **}/{@code *} - matches any path at any level.</li>
     * <li>{@code *} - matches any path at any level, shorthand for {@code {@literal **}/
     * {@literal *}.</li>
     * </ul>
     *
     * <h2>Variables</h2>
     * <p>
     * Jooby supports path parameters too:
     * </p>
     * <p>
     * Some examples:
     * </p>
     * <ul>
     * <li><code> /user/{id}</code> - /user/* and give you access to the <code>id</code> var.</li>
     * <li><code> /user/:id</code> - /user/* and give you access to the <code>id</code> var.</li>
     * <li><code> /user/{id:\\d+}</code> - /user/[digits] and give you access to the numeric
     * <code>id</code> var.</li>
     * </ul>
     *
     * @return A path pattern.
     */
    public @Nonnull String pattern() {
      return pattern;
    }

    /**
     * Test if the route matches the given verb, path, content type and accept header.
     *
     * @return A route or an empty optional.
     */
    public @Nonnull Optional<Route> matches(final @Nonnull Request.Verb verb,
        final @Nonnull String path, final @Nonnull MediaType contentType,
        final @Nonnull List<MediaType> accept) {
      RouteMatcher matcher = compiledPattern.matcher(verb.value() + path);
      if (matcher.matches()) {
        List<MediaType> result = MediaType.matcher(accept).filter(this.produces);
        if (canConsume(contentType) && result.size() > 0) {
          // keep accept when */*
          List<MediaType> produces = result.size() == 1 && result.get(0).name().equals("*/*")
              ? accept : this.produces;
          return Optional.of(asRoute(verb, matcher, produces));
        }
      }
      return Optional.empty();
    }

    /**
     * @return HTTP verb or <code>*</code>
     */
    public @Nonnull String verb() {
      return verb;
    }

    /**
     * @return Route name. Default is: <code>anonymous</code>.
     */
    public @Nonnull String name() {
      return name;
    }

    /**
     * Set the route name.
     *
     * @param name A route's name.
     * @return This definition.
     */
    public @Nonnull Definition name(final @Nonnull String name) {
      checkArgument(!Strings.isNullOrEmpty(name), "A route's name is required.");
      this.name = name;
      return this;
    }

    /**
     * Test if the route definition can consume a media type.
     *
     * @param type A media type to test.
     * @return True, if the route can consume the given media type.
     */
    public boolean canConsume(@Nonnull final MediaType type) {
      return MediaType.matcher(Arrays.asList(type)).matches(consumes);
    }

    /**
     * Test if the route definition can consume a media type.
     *
     * @param types A media types to test.
     * @return True, if the route can produces the given media type.
     */
    public boolean canProduce(final List<MediaType> types) {
      return MediaType.matcher(types).matches(produces);
    }

    /**
     * Set the media types the route can consume.
     *
     * @param consumes The media types to test for.
     * @return This route definition.
     */
    public Definition consumes(final MediaType... consumes) {
      return consumes(Arrays.asList(consumes));
    }

    /**
     * Set the media types the route can consume.
     *
     * @param consumes The media types to test for.
     * @return This route definition.
     */
    public Definition consumes(final List<MediaType> consumes) {
      checkArgument(consumes != null && consumes.size() > 0, "Consumes types are required");
      if (consumes.size() > 1) {
        this.consumes = Lists.newLinkedList(consumes);
        Collections.sort(this.consumes);
      } else {
        this.consumes = ImmutableList.of(consumes.get(0));
      }
      return this;
    }

    /**
     * Set the media types the route can produces.
     *
     * @param produces The media types to test for.
     * @return This route definition.
     */
    public Definition produces(final MediaType... produces) {
      return produces(Arrays.asList(produces));
    }

    /**
     * Set the media types the route can produces.
     *
     * @param produces The media types to test for.
     * @return This route definition.
     */
    public Definition produces(final List<MediaType> produces) {
      checkArgument(produces != null && produces.size() > 0, "Produces types are required");
      if (produces.size() > 1) {
        this.produces = Lists.newLinkedList(produces);
        Collections.sort(this.produces);
      } else {
        this.produces = ImmutableList.of(produces.get(0));
      }
      return this;
    }

    /**
     * @return All the types this route can consumes.
     */
    public List<MediaType> consumes() {
      return ImmutableList.copyOf(this.consumes);
    }

    /**
     * @return All the types this route can produces.
     */
    public List<MediaType> produces() {
      return ImmutableList.copyOf(this.produces);
    }

    @Override
    public String toString() {
      StringBuilder buffer = new StringBuilder();
      buffer.append(verb()).append(" ").append(pattern()).append("\n");
      buffer.append("  name: ").append(name()).append("\n");
      buffer.append("  consume: ").append(consumes()).append("\n");
      buffer.append("  produces: ").append(produces()).append("\n");
      return buffer.toString();
    }

    /**
     * Creates a new route.
     *
     * @param verb A HTTP verb.
     * @param matcher A route matcher.
     * @param produces List of produces types.
     * @return A new route.
     */
    private Route asRoute(final Verb verb, final RouteMatcher matcher,
        final List<MediaType> produces) {
      return new RouteImpl(filter, verb, matcher.path(), pattern, name, matcher.vars(), consumes,
          produces);
    }

  }

  /**
   * A forwarding route.
   *
   * @author edgar
   * @since 0.1.0
   */
  class Forwarding implements Route {

    /**
     * Target route.
     */
    private final Route route;

    /**
     * Creates a new {@link Forwarding} route.
     *
     * @param route A target route.
     */
    public Forwarding(final Route route) {
      this.route = requireNonNull(route, "A route is required.");
    }

    @Override
    public String path() {
      return route.path();
    }

    @Override
    public Request.Verb verb() {
      return route.verb();
    }

    @Override
    public String pattern() {
      return route.pattern();
    }

    @Override
    public String name() {
      return route.name();
    }

    @Override
    public Map<String, String> vars() {
      return route.vars();
    }

    @Override
    public List<MediaType> consumes() {
      return route.consumes();
    }

    @Override
    public List<MediaType> produces() {
      return route.produces();
    }

    @Override
    public String toString() {
      return route.toString();
    }

    /**
     * Find a target route.
     *
     * @param route A route to check.
     * @return A target route.
     */
    public static @Nonnull Route unwrap(final @Nonnull Route route) {
      requireNonNull(route, "A route is required.");
      Route root = route;
      while (root instanceof Forwarding) {
        root = ((Forwarding) root).route;
      }
      return root;
    }

  }

  /**
   * Chain of routes to be executed. It invokes the next route in the chain.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface Chain {
    /**
     * Invokes the next route in the chain.
     *
     * @param req A HTTP request.
     * @param res A HTTP response.
     * @throws Exception If invocation goes wrong.
     */
    void next(@Nonnull Request req, @Nonnull Response res) throws Exception;
  }

  /**
   * An exception that carry a {@link Response.Status}. The status field will be set in the HTTP
   * response.
   *
   * See {@link Err.Handler} for more details on how to deal with exceptions.
   *
   * @author edgar
   * @since 0.1.0
   */
  @SuppressWarnings("serial")
  public class Err extends RuntimeException {

    /**
     * Default err handler with content negotation. If an error is found when accept header is
     * <code>text/html</code> this err handler will delegate err rendering to a template processor.
     *
     * @author edgar
     * @since 0.1.0
     */
    public static class Default implements Err.Handler {

      @Override
      public void handle(final Request req, final Response res, final Exception ex)
          throws Exception {
        LoggerFactory.getLogger(Route.Err.class).error("execution of: " + req.path() +
            " resulted in exception", ex);

        Map<String, Object> err = err(req, res, ex);

        res.format()
            .when(MediaType.html, () -> Viewable.of(errPage(req, res, ex), err))
            .when(MediaType.all, () -> err)
            .send();
      }

    }

    /**
     * Route error handler, it creates a default err model and default view name.
     *
     * The default err handler does content negotation on error, see {@link Default}.
     *
     * @author edgar
     * @since 0.1.0
     */
    @Beta
    public interface Handler {

      /**
       * Build a err model from exception. The model it's map with the following attributes:
       *
       * <pre>
       *   message: String
       *   stacktrace: String[]
       *   status: int
       *   reason: String
       *   referer: String
       * </pre>
       *
       * <p>
       * NOTE: {@link Response#status()} it was set by default to status code > 400. This is the
       * default behavior you can use the generated status code and/or override it.
       * </p>
       *
       * @param req A HTTP Request.
       * @param res A HTTP Response with a default err status code (> 400).
       * @param ex Current exception object.
       * @return A err model.
       */
      default Map<String, Object> err(final Request req, final Response res, final Exception ex) {
        Map<String, Object> error = new LinkedHashMap<>();
        Response.Status status = res.status().get();
        String message = ex.getMessage();
        message = message == null ? status.reason() : message;
        error.put("message", message);
        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));
        String[] stacktrace = writer.toString().replace("\r", "").split("\\n");
        error.put("stacktrace", stacktrace);
        error.put("status", status.value());
        error.put("reason", status.reason());
        error.put("referer", req.header("referer"));

        return error;
      }

      /**
       * Convert current err to a view location, defaults is: <code>/err</code>.
       *
       * @param req HTTP request.
       * @param res HTTP Response.
       * @param ex Error found.
       * @return An err page to be render by a template processor.
       */
      default String errPage(final Request req, final Response res, final Exception ex) {
        return "/err";
      }

      /**
       * Handle a route exception by probably logging the error and sending a err response to the
       * client.
       *
       * @param req HTTP request.
       * @param res HTTP response.
       * @param ex Error found.
       * @throws Exception If something goes wrong.
       */
      void handle(Request req, Response res, Exception ex) throws Exception;
    }

    /**
     * The HTTP status. Required.
     */
    private Response.Status status;

    /**
     * Creates a new {@link Route.Err}.
     *
     * @param status A HTTP status. Required.
     * @param message A error message. Required.
     * @param cause The cause of the problem.
     */
    public Err(final Response.Status status, final String message, final Exception cause) {
      super(message(status, message), cause);
      this.status = status;
    }

    /**
     * Creates a new {@link Route.Err}.
     *
     * @param status A HTTP status. Required.
     * @param message A error message. Required.
     */
    public Err(final Response.Status status, final String message) {
      super(message(status, message));
      this.status = status;
    }

    /**
     * Creates a new {@link Route.Err}.
     *
     * @param status A HTTP status. Required.
     * @param cause The cause of the problem.
     */
    public Err(final Response.Status status, final Exception cause) {
      super(message(status, ""), cause);
      this.status = status;
    }

    /**
     * Creates a new {@link Route.Err}.
     *
     * @param status A HTTP status. Required.
     */
    public Err(final Response.Status status) {
      super(message(status, ""));
      this.status = status;
    }

    /**
     * @return The HTTP status to send as response.
     */
    public Response.Status status() {
      return status;
    }

    /**
     * Build an error message using the HTTP status.
     *
     * @param status The HTTP Status.
     * @param tail A message to append.
     * @return An error message.
     */
    private static String message(final Response.Status status, final String tail) {
      requireNonNull(status, "A HTTP Status is required.");
      return status.reason() + "(" + status.value() + "): " + tail;
    }
  }

  /**
   * @return Current request path.
   */
  String path();

  /**
   * @return Current request verb.
   */
  Request.Verb verb();

  /**
   * @return The currently matched pattern.
   */
  String pattern();

  /**
   * @return Route name, defaults to <code>"anonymous"</code>
   */
  String name();

  /**
   * @return The currently matched path variables (if any).
   */
  Map<String, String> vars();

  /**
   * @return List all the types this route can consumes, defaults is: {@code * / *}.
   */
  List<MediaType> consumes();

  /**
   * @return List all the types this route can produces, defaults is: {@code * / *}.
   */
  List<MediaType> produces();

}

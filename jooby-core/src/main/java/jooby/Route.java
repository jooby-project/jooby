package jooby;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import jooby.internal.RouteImpl;
import jooby.internal.RouteMatcher;
import jooby.internal.RoutePattern;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

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
 * <li><code>com/{@literal **}/test.html</code> - matches all {@code test.html} files underneath the
 * {@code com} path</li>
 * </ul>
 *
 * <h1>Variable Path Patterns</h1>
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
   *        get("/", (req, resp) -> resp.send("GET"));
   *
   *        post("/", (req, resp) -> resp.send("POST"));
   *
   *        put("/", (req, resp) -> resp.send("PUT"));
   *
   *        delete("/", (req, resp) -> resp.status(HttpStatus.NO_CONTENT));
   *     }
   *   }
   * </pre>
   *
   * <h1>Setting what a route can consumes</h1>
   * <p>
   * It's pretty straight forward:
   * </p>
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
   * <p>
   * It's pretty straight forward:
   * </p>
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
   * @author edgar
   * @since 0.1.0
   */
  @Beta
  public class Definition {

    private static final List<MediaType> ALL = ImmutableList.of(MediaType.all);

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
    private List<MediaType> consumes = ALL;

    /**
     * Defines the media types that the methods of a resource class or can produces. Default is:
     * {@literal *}/{@literal *}.
     */
    private List<MediaType> produces = ALL;
    private String verb;

    private String pattern;

    public Definition(final String verb, final String pattern, final Router router) {
      this(verb, pattern, (req, res, chain) -> {
        router.handle(req, res);
        chain.next(req, res);
      });
    }

    /**
     * Creates a new {@link RouteDefinitionImpl}.
     *
     * @param verb A HTTP verb.
     * @param pattern
     * @param filter
     */
    public Definition(final String verb, final String pattern, final Filter filter) {
      requireNonNull(verb, "A HTTP verb is required.");
      requireNonNull(pattern, "A route path is required.");
      requireNonNull(filter, "A filter is required.");

      this.verb = verb;
      this.compiledPattern = new RoutePattern(verb, pattern);
      // normalized pattern
      this.pattern = compiledPattern.pattern();
      this.filter = filter;
    }

    public String pattern() {
      return pattern;
    }

    public Optional<Route> matches(final String verb, final String path, final MediaType contentType,
        final List<MediaType> accept) {
      RouteMatcher matcher = compiledPattern.matcher(verb.toUpperCase() + path);
      if (matcher.matches()) {
        List<MediaType> result = MediaType.matcher(accept).filter(this.produces);
        if (canConsume(contentType) && result.size() > 0) {
          // keep accept when */*
          List<MediaType> produces = result.size() == 1 && result.get(0).name().equals("*/*")
              ? accept : this.produces;
          return Optional.of(asRoute(matcher, produces));
        }
      }
      return Optional.empty();
    }

    public String verb() {
      return verb;
    }

    public String name() {
      return name;
    }

    public Definition name(final String name) {
      this.name = requireNonNull(name, "A route's name is required.");
      return this;
    }

    /**
     * @param candidate A media type to test.
     * @return True, if the route can consume the given media type.
     */
    public boolean canConsume(@Nonnull final MediaType candidate) {
      return MediaType.matcher(Arrays.asList(candidate)).matches(consumes);
    }

    /**
     * @param candidates A media types to test.
     * @return True, if the route can produces the given media type.
     */
    public boolean canProduce(final List<MediaType> candidates) {
      return MediaType.matcher(candidates).matches(produces);
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
    public Definition consumes(final Iterable<MediaType> consumes) {
      this.consumes = Lists.newArrayList(consumes);
      Collections.sort(this.consumes);
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
    public Definition produces(final Iterable<MediaType> produces) {
      this.produces = Lists.newArrayList(produces);
      Collections.sort(this.produces);
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

    private Route asRoute(final RouteMatcher matcher, final List<MediaType> produces) {
      return new RouteImpl(filter, verb, matcher.path(), pattern, name, matcher.vars(), consumes,
          produces);
    }

  }

  interface Chain {
    void next(Request request, Response response) throws Exception;
  }

  String path();

  String verb();

  String pattern();

  String name();

  Map<String, String> vars();

  List<MediaType> consume();

  List<MediaType> produces();

}

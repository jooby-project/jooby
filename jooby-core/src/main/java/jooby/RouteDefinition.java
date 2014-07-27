package jooby;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.annotations.Beta;

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
public interface RouteDefinition {

  /**
   * @return A route pattern.
   */
  RoutePattern path();

  /**
   * @return Target route.
   */
  Route route();

  /**
   * Construct a new {@link RouteMatcher}.
   *
   * @param path The path to test.
   * @return A new route matcher.
   */
  RouteMatcher matcher(String path);

  /**
   * @param candidate A media type to test.
   * @return True, if the route can consume the given media type.
   */
  boolean canConsume(@Nonnull MediaType candidate);

  /**
   * @param candidates A media types to test.
   * @return True, if the route can produces the given media type.
   */
  boolean canProduce(List<MediaType> candidates);

  /**
   * Set the media types the route can consume.
   *
   * @param consumes The media types to test for.
   * @return This route definition.
   */
  RouteDefinition consumes(MediaType... consumes);

  /**
   * Set the media types the route can consume.
   *
   * @param consumes The media types to test for.
   * @return This route definition.
   */
  RouteDefinition consumes(Iterable<MediaType> consumes);

  /**
   * Set the media types the route can produces.
   *
   * @param produces The media types to test for.
   * @return This route definition.
   */
  RouteDefinition produces(MediaType... produces);

  /**
   * Set the media types the route can produces.
   *
   * @param produces The media types to test for.
   * @return This route definition.
   */
  RouteDefinition produces(Iterable<MediaType> produces);

  /**
   * @return All the types this route can consumes.
   */
  List<MediaType> consumes();

  /**
   * @return All the types this route can produces.
   */
  List<MediaType> produces();

}

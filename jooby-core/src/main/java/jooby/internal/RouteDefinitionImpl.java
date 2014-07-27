package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jooby.MediaType;
import jooby.Route;
import jooby.RouteDefinition;
import jooby.RouteMatcher;
import jooby.RoutePattern;

import com.google.common.collect.Lists;

/**
 * DSL for customize routes.
 *
 * @author edgar
 * @since 0.1.0
 */
public class RouteDefinitionImpl implements RouteDefinition {

  /**
   * A route path.
   */
  private RoutePattern path;

  /**
   * The target route.
   */
  private Route route;

  /**
   * Defines the media types that the methods of a resource class or can accept. Default is:
   * {@literal *}/{@literal *}.
   */
  private List<MediaType> consumes = new ArrayList<>();

  /**
   * Defines the media types that the methods of a resource class or can produces. Default is:
   * {@literal *}/{@literal *}.
   */
  private List<MediaType> produces = new ArrayList<>();

  /**
   * Creates a new {@link RouteDefinitionImpl}.
   *
   * @param verb A HTTP verb.
   * @param path
   * @param route
   */
  public RouteDefinitionImpl(final String verb, final String path, final Route route) {
    this(new RoutePatternImpl(verb, path), route);
  }

  private RouteDefinitionImpl(final RoutePatternImpl path, final Route route) {
    this.path = requireNonNull(path, "A path is required.");
    this.route = requireNonNull(route, "A route is required.");
    consumes.add(MediaType.all);
    produces.add(MediaType.all);
  }

  @Override
  public RoutePattern path() {
    return path;
  }

  @Override
  public Route route() {
    return route;
  }

  @Override
  public RouteMatcher matcher(final String path) {
    return this.path.matcher(path);
  }

  @Override
  public boolean canConsume(final MediaType candidate) {
    return MediaType.matcher(Arrays.asList(candidate)).matches(consumes);
  }

  @Override
  public boolean canProduce(final List<MediaType> candidates) {
    return MediaType.matcher(candidates).matches(produces);
  }

  @Override
  public RouteDefinition consumes(final MediaType... supported) {
    return consumes(Arrays.asList(supported));
  }

  @Override
  public RouteDefinitionImpl consumes(final Iterable<MediaType> consumes) {
    this.consumes = Lists.newArrayList(consumes);
    return this;
  }

  @Override
  public RouteDefinition produces(final MediaType... types) {
    return produces(Arrays.asList(types));
  }

  @Override
  public RouteDefinition produces(final Iterable<MediaType> produces) {
    this.produces = Lists.newArrayList(produces);
    return this;
  }

  @Override
  public List<MediaType> consumes() {
    return consumes;
  }

  @Override
  public List<MediaType> produces() {
    return produces;
  }

  @Override
  public String toString() {
    return path.toString() + " " + consumes + " -> " + produces;
  }

}

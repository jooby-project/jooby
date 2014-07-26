package jooby;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jooby.internal.RoutePath;

import com.google.common.collect.Lists;

public class RouteDefinition {

  private RoutePath path;

  private Route route;

  private List<MediaType> consumes = new ArrayList<>();

  private List<MediaType> produces = new ArrayList<>();

  public RouteDefinition(final String method, final String path, final Route route) {
    this(new RoutePath(method, path), route);
  }

  private RouteDefinition(final RoutePath path, final Route route) {
    this.path = requireNonNull(path, "A path is required.");
    this.route = requireNonNull(route, "A route is required.");
    consumes.add(MediaType.all);
    produces.add(MediaType.all);
  }

  public RoutePath path() {
    return path;
  }

  public Route route() {
    return route;
  }

  public RouteMatcher matcher(final String path) {
    return this.path.matcher(path);
  }

  public boolean canConsume(final MediaType candidate) {
    return MediaType.matcher(Arrays.asList(candidate)).matches(consumes);
  }

  public boolean canProduce(final List<MediaType> candidates) {
    return MediaType.matcher(candidates).matches(produces);
  }

  public RouteDefinition consumes(final MediaType... supported) {
    return consumes(Arrays.asList(supported));
  }

  public RouteDefinition consumes(final Iterable<MediaType> consumes) {
    this.consumes = Lists.newArrayList(consumes);
    return this;
  }

  public RouteDefinition produces(final MediaType... types) {
    return produces(Arrays.asList(types));
  }

  public RouteDefinition produces(final Iterable<MediaType> produces) {
    this.produces = Lists.newArrayList(produces);
    return this;
  }

  public List<MediaType> consumes() {
    return consumes;
  }

  public List<MediaType> produces() {
    return produces;
  }

  @Override
  public String toString() {
    return path.toString() + " " + consumes + " -> " + produces;
  }

}

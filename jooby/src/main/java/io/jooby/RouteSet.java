/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Optional.ofNullable;

/**
 * Give you access to all routes created inside a {@link Router#path(String, Runnable)}.
 * Allow to globally apply attributes or metadata.
 *
 * @author edgar
 * @since 2.7.3
 */
public class RouteSet {

  private List<Route> routes;

  private List<String> tags = EMPTY_LIST;

  private String summary;

  private String description;

  /**
   * Sub-routes. Always empty except when used it from {@link Router#path(String, Runnable)} or
   * {@link Router#routes(Runnable)}.
   *
   * @return Sub-routes.
   */
  public @Nonnull List<Route> getRoutes() {
    return routes;
  }

  /**
   * Set sub-routes.
   *
   * @param routes Sub-routes.
   * @return This route.
   */
  public @Nonnull RouteSet setRoutes(@Nonnull List<Route> routes) {
    this.routes = routes;
    return this;
  }

  /**
   * Add one or more response types (format) produces by this route.
   *
   * @param produces Produce types.
   * @return This route.
   */
  public @Nonnull RouteSet produces(@Nonnull MediaType... produces) {
    return setProduces(Arrays.asList(produces));
  }

  /**
   * Add one or more response types (format) produces by this route.
   *
   * @param produces Produce types.
   * @return This route.
   */
  public @Nonnull RouteSet setProduces(@Nonnull Collection<MediaType> produces) {
    routes.forEach(it -> {
      if (it.getProduces().isEmpty()) {
        it.setProduces(produces);
      }
    });
    return this;
  }

  /**
   * Add one or more request types (format) consumed by this route.
   *
   * @param consumes Consume types.
   * @return This route.
   */
  public @Nonnull RouteSet consumes(@Nonnull MediaType... consumes) {
    return setConsumes(Arrays.asList(consumes));
  }

  /**
   * Add one or more request types (format) consumed by this route.
   *
   * @param consumes Consume types.
   * @return This route.
   */
  public @Nonnull RouteSet setConsumes(@Nonnull Collection<MediaType> consumes) {
    routes.forEach(it -> {
      if (it.getConsumes().isEmpty()) {
        it.setConsumes(consumes);
      }
    });
    return this;
  }

  /**
   * Add one or more attributes applied to this route.
   *
   * @param attributes .
   * @return This route.
   */
  public @Nonnull RouteSet setAttributes(@Nonnull Map<String, Object> attributes) {
    routes.forEach(it -> attributes.forEach((k, v) -> it.getAttributes().putIfAbsent(k, v)));
    return this;
  }

  /**
   * Add one or more attributes applied to this route.
   *
   * @param name attribute name
   * @param value attribute value
   * @return This route.
   */
  public @Nonnull RouteSet attribute(@Nonnull String name, @Nonnull Object value) {
    routes.forEach(it -> it.getAttributes().putIfAbsent(name, value));
    return this;
  }

  /**
   * Set executor key. The route is going to use the given key to fetch an executor. Possible values
   * are:
   *
   * - <code>null</code>: no specific executor, uses the default Jooby logic to choose one, based
   *    on the value of {@link ExecutionMode};
   * - <code>worker</code>: use the executor provided by the server.
   * - <code>arbitrary name</code>: use an named executor which as registered using
   *    {@link Router#executor(String, Executor)}.
   *
   * @param executorKey Executor key.
   * @return This route.
   */
  public @Nonnull RouteSet setExecutorKey(@Nullable String executorKey) {
    routes.forEach(it -> it.setExecutorKey(ofNullable(it.getExecutorKey()).orElse(executorKey)));
    return this;
  }

  /**
   * Route tags.
   *
   * @return Route tags.
   */
  public @Nonnull List<String> getTags() {
    return tags;
  }

  /**
   * Tag this route. Tags are used for documentation purpose from openAPI generator.
   *
   * @param tags Tags.
   * @return This route.
   */
  public @Nonnull RouteSet setTags(@Nonnull List<String> tags) {
    if (this.tags == EMPTY_LIST) {
      this.tags = new ArrayList<>();
    }
    routes.forEach(it -> tags.forEach(it::addTag));
    return this;
  }

  /**
   * Tag this route. Tags are used for documentation purpose from openAPI generator.
   *
   * @param tags Tags.
   * @return This route.
   */
  public @Nonnull RouteSet tags(@Nonnull String... tags) {
    return setTags(Arrays.asList(tags));
  }

  /**
   * Route summary useful for documentation purpose from openAPI generator.
   *
   * @return Summary.
   */
  public @Nullable String getSummary() {
    return summary;
  }

  /**
   * Route summary useful for documentation purpose from openAPI generator.
   *
   * @param summary Summary.
   * @return This route.
   */
  public @Nonnull RouteSet summary(@Nullable String summary) {
    return setSummary(summary);
  }

  /**
   * Route summary useful for documentation purpose from openAPI generator.
   *
   * @param summary Summary.
   * @return This route.
   */
  public @Nonnull RouteSet setSummary(@Nullable String summary) {
    this.summary = summary;
    return this;
  }

  /**
   * Route description useful for documentation purpose from openAPI generator.
   *
   * @return Route description.
   */
  public @Nullable String getDescription() {
    return description;
  }

  /**
   * Route description useful for documentation purpose from openAPI generator.
   *
   * @param description Description.
   * @return This route.
   */
  public @Nonnull RouteSet setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Route description useful for documentation purpose from openAPI generator.
   *
   * @param description Description.
   * @return This route.
   */
  public @Nonnull RouteSet description(@Nullable String description) {
    return setDescription(description);
  }
}

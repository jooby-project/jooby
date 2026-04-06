/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3863;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.annotation.*;
import io.jooby.annotation.trpc.Trpc;
import io.jooby.exception.NotFoundException;
import reactor.core.publisher.Mono;

@Trpc("movies")
@Path("/api/movies")
public class MovieServiceTs {

  private final List<Movie> database =
      List.of(new Movie(1, "The Godfather", 1972), new Movie(2, "Pulp Fiction", 1994));

  /** Procedure: movies.create Takes a single complex object. */
  @Trpc.Mutation
  @POST("/create")
  public Movie create(Movie movie) {
    // In a real app, save to DB. For now, just return it.
    return movie;
  }

  /** Procedure: movies.create Takes a single complex object. */
  @Trpc.Mutation
  public Mono<Movie> createMono(Movie movie) {
    // In a real app, save to DB. For now, just return it.
    return Mono.just(movie);
  }

  @Trpc.Mutation
  @PUT
  public void resetIndex() {}

  /** Procedure: movies.bulkCreate Takes a List of complex objects. */
  @Trpc.Query
  public List<String> bulkCreate(List<Movie> movies) {
    return movies.stream().map(m -> "Created: " + m.title()).collect(Collectors.toList());
  }

  /** Procedure: movies.ping */
  @Trpc.Query
  public String ping() {
    return "pong";
  }

  /** Procedure: movies.getById Single primitive argument */
  @Trpc
  @GET("/{id}")
  public @NonNull Movie getById(@PathParam int id) {
    return database.stream()
        .filter(m -> m.id() == id)
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Movie not found: " + id));
  }

  /** Procedure: movies.search Multi-argument (Tuple) */
  @Trpc.Query
  public List<Movie> search(String title, Integer year) {
    return database.stream()
        .filter(m -> m.title().contains(title) && (year == null || m.year() == year))
        .toList();
  }

  /** Procedure: movies.addReview Mix of String and int (Mutation) */
  @Trpc.Mutation
  public Map<String, Object> addReview(String movieTitle, int stars, String comment) {
    // Business logic...
    return Map.of(
        "title", movieTitle,
        "rating", stars,
        "status", "published");
  }

  /** Procedure: movies.addReview Mix of String and int (Mutation) */
  @Trpc
  @PUT("/{id}/metadata")
  public Metadata updateMetadata(@PathParam int id, Metadata metadata) {
    // Business logic...
    return metadata;
  }

  @Trpc
  @DELETE("/{id}")
  public void deleteMovie(@PathParam int id) {}
}

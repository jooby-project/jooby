/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3863;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.jooby.annotation.Trpc;

@Trpc("movies")
public class MovieService {

  private final List<Movie> database =
      List.of(new Movie(1, "The Godfather", 1972), new Movie(2, "Pulp Fiction", 1994));

  /** Procedure: movies.create Takes a single complex object. */
  @Trpc.Mutation
  public Movie create(Movie movie) {
    // In a real app, save to DB. For now, just return it.
    return movie;
  }

  @Trpc.Mutation
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
  @Trpc.Query
  public Movie getById(int id) {
    return database.stream().filter(m -> m.id() == id).findFirst().orElse(null);
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
  @Trpc.Mutation
  public Metadata updateMetadata(int id, Metadata metadata) {
    // Business logic...
    return metadata;
  }
}

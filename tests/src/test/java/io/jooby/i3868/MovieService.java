/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3868;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.annotation.*;
import io.jooby.exception.NotFoundException;
import io.jooby.i3863.Movie;

@JsonRpc("movies")
public class MovieService {

  private final List<Movie> database =
      List.of(new Movie(1, "The Godfather", 1972), new Movie(2, "Pulp Fiction", 1994));

  /** Procedure: movies.create Takes a single complex object. */
  public Movie create(Movie movie) {
    // In a real app, save to DB. For now, just return it.
    return movie;
  }

  public @NonNull Movie getById(int id) {
    return database.stream()
        .filter(m -> m.id() == id)
        .findFirst()
        .orElseThrow(() -> new NotFoundException("Movie not found: " + id));
  }

  public List<Movie> search(String title, Integer year) {
    return database.stream()
        .filter(m -> m.title().contains(title) && (year == null || m.year() == year))
        .toList();
  }

  public void deleteMovie(@PathParam int id) {}
}

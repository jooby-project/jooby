/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i3868

import io.jooby.annotation.*
import io.jooby.annotation.jsonrpc.JsonRpc
import io.jooby.exception.NotFoundException
import io.jooby.i3863.Movie
import java.util.function.Supplier

@JsonRpc("movies")
@Path("/api/movies")
class KtMovieServiceRpc {
  private val database: List<Movie> =
    listOf(Movie(1, "The Godfather", 1972), Movie(2, "Pulp Fiction", 1994))

  /** Procedure: movies.create Takes a single complex object. */
  @POST
  @JsonRpc
  fun create(movie: Movie): Movie {
    // In a real app, save to DB. For now, just return it.
    return movie
  }

  fun getById(id: Int): Movie {
    return database
      .stream()
      .filter { m: Movie -> m.id == id }
      .findFirst()
      .orElseThrow<NotFoundException?>(Supplier { NotFoundException("Movie not found: " + id) })
  }

  fun search(title: String, year: Int): List<Movie> {
    return database.stream().filter { m -> m.title.contains(title) && (m.year == year) }.toList()
  }

  fun deleteMovie(id: Int) {}
}

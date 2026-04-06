/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package i3863

import io.jooby.annotation.trpc.Trpc
import io.jooby.i3863.Metadata
import io.jooby.i3863.Movie
import java.util.stream.Collectors

@Trpc("movies")
class KMovieService {
  private val database =
    listOf<Movie?>(Movie(1, "The Godfather", 1972), Movie(2, "Pulp Fiction", 1994))

  /** Procedure: movies.create Takes a single complex object. */
  @Trpc.Mutation
  fun create(movie: Movie?): Movie? {
    // In a real app, save to DB. For now, just return it.
    return movie
  }

  @Trpc.Mutation fun resetIndex() {}

  /** Procedure: movies.bulkCreate Takes a List of complex objects. */
  @Trpc.Query
  fun bulkCreate(movies: List<Movie>): List<String> {
    return movies
      .stream()
      .map<String?> { m: Movie -> "Created: " + m.title }
      .collect(Collectors.toList())
  }

  /** Procedure: movies.ping */
  @Trpc.Query
  fun ping(): String {
    return "pong"
  }

  /** Procedure: movies.getById Single primitive argument */
  @Trpc.Query
  fun getById(id: Int): Movie? {
    return database.stream().filter { m: Movie? -> m!!.id == id }.findFirst().orElse(null)
  }

  /** Procedure: movies.search Multi-argument (Tuple) */
  @Trpc.Query
  fun search(title: String, year: Int?): List<Movie?> {
    return database
      .stream()
      .filter { m: Movie? -> m!!.title.contains(title) && (year == null || m.year == year) }
      .toList()
  }

  /** Procedure: movies.addReview Mix of String and int (Mutation) */
  @Trpc.Mutation
  fun addReview(movieTitle: String, stars: Int, comment: String?): Map<String, Any> {
    // Business logic...
    return mapOf("title" to movieTitle, "rating" to stars, "status" to "published")
  }

  /** Procedure: movies.addReview Mix of String and int (Mutation) */
  @Trpc.Mutation
  fun updateMetadata(id: Int, metadata: Metadata?): Metadata? {
    // Business logic...
    return metadata
  }
}

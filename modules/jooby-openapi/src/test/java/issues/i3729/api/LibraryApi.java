/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

import io.jooby.annotation.GET;
import io.jooby.annotation.POST;
import io.jooby.annotation.Path;
import io.jooby.annotation.PathParam;

/**
 * Library API.
 *
 * <p>Contains all operations for creating, updating and fetching books.
 */
@Path("/api/library")
public class LibraryApi {

  /**
   * Find a book by isbn.
   *
   * @param isbn Book isbn. Like IK-1900.
   * @return A book
   */
  @GET("/{isbn}")
  public Book bookByIsbn(@PathParam String isbn) {
    return new Book();
  }

  /**
   * Creates a new book.
   *
   * @param book Book to create.
   * @return Saved book.
   */
  @POST
  public Book createBook(Book book) {
    return book;
  }
}

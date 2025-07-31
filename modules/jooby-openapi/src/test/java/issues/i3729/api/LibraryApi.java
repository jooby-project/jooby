/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

import java.util.List;

import io.jooby.annotation.*;
import io.jooby.exception.BadRequestException;
import io.jooby.exception.NotFoundException;

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
   * @return A matching book.
   * @throws NotFoundException <code>404</code> If a book doesn't exist.
   * @throws BadRequestException <code>400</code> For bad ISBN code.
   */
  @GET("/{isbn}")
  public Book bookByIsbn(@PathParam String isbn) throws NotFoundException, BadRequestException {
    return new Book();
  }

  /**
   * Query books.
   *
   * @param query Book's param query.
   * @return Matching books.
   */
  @GET
  public List<Book> query(@QueryParam BookQuery query) {
    return List.of(new Book());
  }

  /**
   * Creates a new book.
   *
   * <p>Book can be created or updated.
   *
   * @param book Book to create.
   * @return Saved book.
   */
  @POST
  public Book createBook(Book book) {
    return book;
  }
}

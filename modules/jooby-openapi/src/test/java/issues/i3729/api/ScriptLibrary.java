/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

import java.util.ArrayList;
import java.util.List;

import io.jooby.Context;
import io.jooby.Jooby;

/**
 * Library API.
 *
 * <p>Available data: Books and authors.
 *
 * @version 4.0.0
 * @server.url https://api.fake-museum-example.com/v1
 * @contact.name Jooby
 * @contact.url https://jooby.io
 * @license.name Apache
 * @contact.email support@jooby.io
 * @license.url https://jooby.io/LICENSE
 * @x-logo.url https://redocly.github.io/redoc/museum-logo.png
 * @x-logo.altText Museum logo
 */
public class ScriptLibrary extends Jooby {

  {
    /*
     * Library API.
     *
     * <p>Contains all operations for creating, updating and fetching books.
     *
     * @tag.name Library
     * @tag.description Access to all books.
     */
    path(
        "/api/library",
        () -> {
          get("/{isbn}", this::bookByIsbn);

          /*
           * Author by Id.
           *
           * @param id ID.
           * @return An author
           * @tag Author. Oxxx
           * @operationId author
           */
          get(
              "/{id}",
              ctx -> {
                var id = ctx.path("id").value();
                return new Author();
              });

          /*
           * Query books.
           *
           * @param query Book's param query.
           * @return Matching books.
           * @x-badges [{name:Beta, position: before, color: purple}]
           * @operationId query
           */
          get(
              "/",
              ctx -> {
                List<Book> result = new ArrayList<>();
                var query = ctx.query(BookQuery.class);
                return result; // List.of(new Book());
              });

          /*
           * Creates a new book.
           *
           * <p>Book can be created or updated.
           *
           * @param book Book to create.
           * @return Saved book.
           * @tag Author
           * @operationId createBook
           */
          post(
              "/",
              ctx -> {
                var book = ctx.body(Book.class);
                return book;
              });
        });
  }

  /*
   * Find a book by isbn.
   *
   * @param isbn Book isbn. Like IK-1900.
   * @return A matching book.
   * @throws NotFoundException <code>404</code> If a book doesn't exist.
   * @throws BadRequestException <code>400</code> For bad ISBN code.
   * @tag Book
   * @tag Author
   * @operationId bookByIsbn
   */
  private Book bookByIsbn(Context ctx) {
    var isbn = ctx.path("isbn").value();
    return new Book();
  }
}

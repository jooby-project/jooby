/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

import java.util.List;

import io.jooby.annotation.*;
import io.jooby.exception.NotFoundException;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;

/**
 * The Public Front Desk of the library.
 *
 * @tag Library. Available library operations.
 */
@Path("/library")
public class LibraryApi2 {

  private final LibraryRepo library;

  @Inject
  public LibraryApi2(LibraryRepo library) {
    this.library = library;
  }

  /**
   * Get Book Details.
   *
   * <p>Call this to show the details page of a specific book.
   *
   * @param isbn The unique ID from the URL (e.g., /books/978-3-16-148410-0)
   * @return The book data
   * @throws NotFoundException <code>404</code> error if it doesn't exist.
   */
  @GET
  @Path("/books/{isbn}")
  public Book getBook(@PathParam String isbn) {
    return library.findBook(isbn).orElseThrow(() -> new NotFoundException(isbn));
  }

  /**
   * Search Books.
   *
   * <p>A general search bar. Users type a word, and we find matches.
   *
   * @param q The search term typed by the user.
   * @return A list of books matching that term.
   */
  @GET
  @Path("/search")
  public List<Book> searchBooks(@QueryParam String q) {
    var pattern = "%" + (q != null ? q : "") + "%";

    return library.searchBooks(pattern);
  }

  /**
   * Browse Books (Paginated).
   *
   * @param title The exact book title to filter by.
   * @param page Which page number to load (defaults to 1).
   * @param size How many books to show per page (defaults to 20).
   * @return A "Page" object containing the books and info like "Total Pages: 5".
   */
  @GET
  @Path("/books")
  public Page<Book> getBooksByTitle(
      @QueryParam String title, @QueryParam int page, @QueryParam int size) {
    // Ensure we have sensible defaults if the user sends nothing
    int pageNum = page > 0 ? page : 1;
    int pageSize = size > 0 ? size : 20;

    // Ask the database for just this specific slice of data
    return library.findBooksByTitle(title, PageRequest.ofPage(pageNum).size(pageSize));
  }

  /**
   * Add New Book. Usage: Send a JSON packet to this URL to create a new book entry in the system.
   *
   * @param book New book to add.
   * @return A text message confirming success.
   */
  @POST
  @Path("/books")
  public Book addBook(Book book) {
    // Save it
    return library.add(book);
  }
}

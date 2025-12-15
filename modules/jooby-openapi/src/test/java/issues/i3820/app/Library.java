/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3820.app;

import java.util.List;
import java.util.Optional;

import issues.i3820.model.*;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.*;

/**
 * The "Librarian" of our system.
 *
 * <p>This interface handles all the work of finding, saving, and removing books and authors from
 * the database. You don't need to write the code for this; the system builds it automatically based
 * on these method names.
 */
public interface Library {

  // --- Finding Items ---

  /**
   * Looks up a single book using its ISBN code.
   *
   * @param isbn The unique code to look for.
   * @return An "Optional" box that contains the book if we found it, or is empty if we didn't.
   */
  @Find
  Optional<Book> findBook(String isbn);

  /** Looks up an author using their ID. */
  @Find
  Optional<Author> findAuthor(String ssn);

  /**
   * Finds books that match a specific title.
   *
   * <p>Because there might be thousands of results, this method splits them into "pages". You ask
   * for "Page 1" or "Page 5", and it gives you just that chunk.
   *
   * @param title The exact title to look for.
   * @param pageRequest Which page of results do you want?
   * @return A page containing a list of books and total count info.
   */
  @Find
  Page<Book> findBooksByTitle(String title, PageRequest pageRequest);

  // --- Custom Searches ---

  /**
   * Search for books that have a specific word in the title.
   *
   * <p>Example: If you search for "%Harry%", it finds "Harry Potter" and "Dirty Harry". It also
   * sorts the results alphabetically by title.
   */
  @Query("where title like :pattern order by title")
  List<Book> searchBooks(String pattern);

  /**
   * A custom report that just lists the titles of new books. Useful for creating quick lists
   * without loading all the book details.
   *
   * @param minYear The oldest year we care about (e.g., 2023).
   * @return Just the names of the books.
   */
  @Query("select title from Book where extract(year from publicationDate) >= :minYear")
  List<String> findRecentBookTitles(int minYear);

  // --- Saving & Deleting ---

  /** Registers a new book in the system. */
  @Insert
  Book add(Book book);

  /** Saves changes made to an author's details. */
  @Update
  void update(Author author);

  /** Permanently removes a book from the library. */
  @Delete
  void remove(Book book);
}

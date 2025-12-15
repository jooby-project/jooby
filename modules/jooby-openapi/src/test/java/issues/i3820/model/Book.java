/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3820.model;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a physical Book in our library.
 *
 * <p>This is the main item visitors look for. It holds details like the title, the actual text
 * content, and who published it.
 */
public class Book {

  /**
   * The unique "barcode" for this book (ISBN). We use this to identify exactly which book edition
   * we are talking about.
   */
  private String isbn;

  /** The name printed on the cover. */
  private String title;

  /** When this book was released to the public. */
  private LocalDate publicationDate;

  /**
   * The full story or content of the book.
   *
   * <p>Since this can be very long, we store it in a special way (Large Object) to keep the
   * database fast.
   */
  private String text;

  /** Categorizes the item (e.g., is it a regular Book or a Magazine?). */
  private BookType type;

  /**
   * The company that published this book.
   *
   * <p>Performance Note: We only load this information if you specifically ask for it ("Lazy"),
   * which saves memory.
   */
  private Publisher publisher;

  /** The list of people who wrote this book. */
  private Set<Author> authors = new HashSet<>();

  public Book() {}

  public Book(String isbn, String title, BookType type) {
    this.isbn = isbn;
    this.title = title;
    this.type = type;
    this.text = "Content placeholder";
  }

  public String getIsbn() {
    return isbn;
  }

  public void setIsbn(String isbn) {
    this.isbn = isbn;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LocalDate getPublicationDate() {
    return publicationDate;
  }

  public void setPublicationDate(LocalDate publicationDate) {
    this.publicationDate = publicationDate;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public BookType getType() {
    return type;
  }

  public void setType(BookType type) {
    this.type = type;
  }

  public Publisher getPublisher() {
    return publisher;
  }

  public void setPublisher(Publisher publisher) {
    this.publisher = publisher;
  }

  public Set<Author> getAuthors() {
    return authors;
  }

  public void setAuthors(Set<Author> authors) {
    this.authors = authors;
  }
}

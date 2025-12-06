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
  private Type type;

  /**
   * The company that published this book.
   *
   * <p>Performance Note: We only load this information if you specifically ask for it ("Lazy"),
   * which saves memory.
   */
  private Publisher publisher;

  /** The list of people who wrote this book. */
  private Set<Author> authors = new HashSet<>();

  /** Defines the format and release schedule of the item. */
  public enum Type {
    /**
     * A fictional narrative story.
     *
     * <p>Examples: "Pride and Prejudice", "Harry Potter", "Dune". These are creative works meant
     * for entertainment or artistic expression.
     */
    NOVEL,

    /**
     * A written account of a real person's life.
     *
     * <p>Examples: "Steve Jobs" by Walter Isaacson, "The Diary of a Young Girl". These are
     * non-fiction historical records of an individual.
     */
    BIOGRAPHY,

    /**
     * An educational book used for study.
     *
     * <p>Examples: "Calculus: Early Transcendentals", "Introduction to Java Programming". These are
     * designed for students and are often used as reference material in academic courses.
     */
    TEXTBOOK,

    /**
     * A periodical publication intended for general readers.
     *
     * <p>Examples: Time, National Geographic, Vogue. These contain various articles, are published
     * frequently (weekly/monthly), and often have a glossy format.
     */
    MAGAZINE,

    /**
     * A scholarly or professional publication.
     *
     * <p>Examples: The New England Journal of Medicine, Harvard Law Review. These focus on academic
     * research or trade news and are written by experts for other experts.
     */
    JOURNAL
  }

  public Book() {}

  public Book(String isbn, String title, Type type) {
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

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
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

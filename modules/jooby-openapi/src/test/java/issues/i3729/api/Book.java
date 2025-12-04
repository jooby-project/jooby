/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

import java.time.LocalDate;
import java.util.Set;

import io.jooby.FileUpload;

/** Book model. */
public class Book {
  /** Book ISBN. */
  private String isbn;

  /** Book's title. */
  String title;

  /** Publication date. Format mm-dd-yyyy. */
  LocalDate publicationDate;

  /** Book's content. */
  String text;

  /** Book type. */
  Type type = Type.Fiction;

  Set<Author> authors;

  FileUpload image;

  /**
   * Book ISBN. Method.
   *
   * @return Book ISBN. Method.
   */
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

  public Set<Author> getAuthors() {
    return authors;
  }

  public void setAuthors(Set<Author> authors) {
    this.authors = authors;
  }

  public FileUpload getImage() {
    return image;
  }

  public void setImage(FileUpload image) {
    this.image = image;
  }
}

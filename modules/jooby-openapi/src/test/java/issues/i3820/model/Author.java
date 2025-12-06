/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3820.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** A person who writes books. */
public class Author {

  /** The author's unique government ID (SSN). */
  public String ssn;

  /** The full name of the author. */
  public String name;

  /**
   * Where the author lives. This information is stored inside the Author table, not a separate one.
   */
  public Address address;

  @JsonIgnore public Set<Book> books = new HashSet<>();

  public Author() {}

  public Author(String ssn, String name) {
    this.ssn = ssn;
    this.name = name;
  }
}

/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3820.model;

/** A company that produces and sells books. */
public class Publisher {
  /**
   * The unique internal ID for this publisher.
   *
   * <p>This is a number generated automatically by the system. Users usually don't need to memorize
   * this, but it's used by the database to link books to their publishers.
   */
  private Long id;

  /**
   * The official business name of the publishing house.
   *
   * <p>Example: "Penguin Random House" or "O'Reilly Media".
   */
  private String name;

  public Publisher() {}

  public Publisher(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }
}

/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3820.model;

/** Defines the format and release schedule of the item. */
public enum BookType {
  /**
   * A fictional narrative story.
   *
   * <p>Examples: "Pride and Prejudice", "Harry Potter", "Dune". These are creative works meant for
   * entertainment or artistic expression.
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

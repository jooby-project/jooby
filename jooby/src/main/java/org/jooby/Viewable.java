package org.jooby;

import static java.util.Objects.requireNonNull;

/**
 * Hold view information like view's name and model.
 *
 * @author edgar
 * @since 0.1.0
 */
public class Viewable {

  /** View's name. */
  private String name;

  /** View's model. */
  private Object model;

  /**
   * Creates a new {@link Viewable}.
   *
   * @param name View's name.
   * @param model View's model.
   */
  public Viewable(final String name, final Object model) {
    this.name = requireNonNull(name, "The name is required.");

    this.model = requireNonNull(model, "The model is required.");
  }

  /**
   * @return View's name.
   */
  public String name() {
    return name;
  }

  /**
   * @return View's model.
   */
  public Object model() {
    return model;
  }

  @Override
  public String toString() {
    return name + ": " + model;
  }

  /**
   * Creates a new {@link Viewable}.
   *
   * @param name View's name.
   * @param model View's model.
   * @return A new viewable.
   */
  public static Viewable of(final String name, final Object model) {
    return new Viewable(name, model);
  }
}

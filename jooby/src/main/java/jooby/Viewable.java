package jooby;

import static java.util.Objects.requireNonNull;

public class Viewable {

  private String name;

  private Object model;

  public Viewable(final String name, final Object model) {
    this.name = requireNonNull(name, "The name is required.");

    this.model = requireNonNull(model, "The model is required.");
  }

  public String name() {
    return name;
  }

  public Object model() {
    return model;
  }

  @Override
  public String toString() {
    return name + ": " + model;
  }

  public static Viewable of(final String name, final Object model) {
    return new Viewable(name, model);
  }
}

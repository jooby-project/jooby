package io.jooby;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class ModelAndView {

  public final String view;

  public final Object model;

  public final Map<String, Object> attributes = new HashMap<>();

  public ModelAndView(@Nonnull String view, @Nonnull Object model) {
    this.view = view;
    this.model = model;
  }

  public ModelAndView put(String name, Object value) {
    attributes.put(name, value);
    return this;
  }

  public ModelAndView put(Map<String, Object> attributes) {
    this.attributes.putAll(attributes);
    return this;
  }
}

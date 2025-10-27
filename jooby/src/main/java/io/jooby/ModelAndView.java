/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Used by template engines to renderer views.
 *
 * @since 2.0.0
 * @author edgar
 * @param <T> Model type.
 */
public class ModelAndView<T> {

  /** Thrown by template engine when they are not capable of rendering a {@link ModelAndView}. */
  public static class UnsupportedModelAndView extends IllegalArgumentException {
    /**
     * Constructor.
     *
     * @param supported List of supported model implementation.
     */
    public UnsupportedModelAndView(Class<?>... supported) {
      super(
          "Only "
              + Set.of(supported).stream().map(Class::getName).collect(Collectors.joining(", "))
              + " are supported");
    }
  }

  /** View name. */
  private final String view;

  /** View data. */
  protected final T model;

  /** Locale used when rendering the view. */
  private Locale locale;

  /**
   * Creates a new model and view.
   *
   * @param view View name must include file extension.
   * @param model View model.
   */
  public ModelAndView(@NonNull String view, @NonNull T model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Creates a model and view backed by a map.
   *
   * @param view View name.
   * @return A map model and view.
   */
  public static MapModelAndView map(@NonNull String view) {
    return new MapModelAndView(view);
  }

  /**
   * Creates a model and view backed by a map.
   *
   * @param view View name.
   * @param model Map instance.
   * @return A map model and view.
   */
  public static MapModelAndView map(@NonNull String view, @NonNull Map<String, Object> model) {
    return new MapModelAndView(view, model);
  }

  /**
   * Sets the locale used when rendering the view, if the template engine supports setting it.
   * Specifying {@code null} triggers a fallback to a locale determined by the current request.
   *
   * @param locale The locale used when rendering the view.
   * @return This instance.
   */
  public ModelAndView<T> setLocale(@Nullable Locale locale) {
    this.locale = locale;
    return this;
  }

  /**
   * View data (a.k.a as model).
   *
   * @return View data (a.k.a as model).
   */
  public T getModel() {
    return model;
  }

  /**
   * View name with file extension, like: <code>index.html</code>.
   *
   * @return View name with file extension, like: <code>index.html</code>.
   */
  public String getView() {
    return view;
  }

  /**
   * Returns the locale used when rendering the view. Defaults to {@code null}, which triggers a
   * fallback to a locale determined by the current request.
   *
   * @return The locale used when rendering the view.
   */
  @Nullable public Locale getLocale() {
    return locale;
  }

  @Override
  public String toString() {
    return view;
  }
}

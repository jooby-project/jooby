/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.htmx;

import java.util.*;

import org.jspecify.annotations.Nullable;

import io.jooby.ModelAndView;

/**
 * A specialized view carrier for HTMX Out-of-Band (OOB) swaps.
 *
 * <p>The HTMX APT generator instantiates this class when a controller method uses {@code @HxOob}
 * annotations alongside a primary {@code @HxView}. It instructs the {@link HtmxTemplateEngine} to
 * sequentially render multiple templates using the same model.
 */
public class HtmxModelAndView<T> extends ModelAndView<T> implements Iterable<ModelAndView<T>> {

  private final Map<String, Object> oobViews = new LinkedHashMap<>();

  /**
   * Creates a new HTMX multi-view.
   *
   * @param primaryView The main template path (e.g., from {@code @HxView}).
   * @param model The data model shared across all templates.
   */
  public HtmxModelAndView(String primaryView, @Nullable T model) {
    super(primaryView, model);
  }

  /**
   * Adds an Out-of-Band view to the rendering pipeline.
   *
   * @param view The OOB template path.
   * @return This instance.
   */
  public HtmxModelAndView<T> addOob(String view) {
    return addOob(view, model);
  }

  /**
   * Adds an Out-of-Band (OOB) view and its associated model to the rendering pipeline.
   *
   * @param view The template path for the OOB view.
   * @param model The data model associated with the specified OOB view.
   * @return The current instance of {@code HtmxModelAndView}.
   */
  public HtmxModelAndView<T> addOob(String view, Object model) {
    this.oobViews.put(view, model);
    return this;
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public Iterator<ModelAndView<T>> iterator() {
    var views = new ArrayList();
    views.add(ModelAndView.of(getView(), model));

    for (var oob : oobViews.entrySet()) {
      views.add(ModelAndView.of(oob.getKey(), oob.getValue()));
    }

    return views.iterator();
  }
}

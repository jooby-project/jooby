/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

/**
 * Hold view information like view's name and model.
 *
 * @author edgar
 * @since 0.1.0
 */
public class View {

  /**
   * Special body serializer for dealing with {@link View}.
   *
   * @author edgar
   * @since 0.1.0
   */
  public interface Engine extends BodyFormatter {

    List<MediaType> HTML = ImmutableList.of(MediaType.html);

    default String name() {
      String name = getClass().getName();
      return name.substring(Math.max(-1, name.lastIndexOf('.')) + 1) .toLowerCase();
    }

    @Override
    default List<MediaType> types() {
      return HTML;
    }

    @Override
    default boolean canFormat(final Class<?> type) {
      return View.class.isAssignableFrom(type);
    }

    @Override
    default void format(final Object body, final BodyFormatter.Context writer) throws Exception {
      final View viewable = (View) body;
      render(viewable, writer);
    }

    /**
     * Render a view.
     *
     * @param viewable View to render.
     * @param writer A body writer.
     * @throws Exception If view rendering fails.
     */
    void render(final View viewable, final BodyFormatter.Context writer) throws Exception;

  }

  /** View's name. */
  private final String name;

  /** View's engine. */
  private String engine = "";

  /** View's model. */
  private final Map<String, Object> model = new HashMap<>();

  /**
   * Creates a new {@link View}.
   *
   * @param name View's name.
   */
  private View(final String name) {
    this.name = requireNonNull(name, "A view name is required.");
  }

  /**
   * @return View's name.
   */
  public String name() {
    return name;
  }

  public View put(final String name, final Object value) {
    requireNonNull(name, "Model name is required.");
    model.put(name, value);
    return this;
  }

  public View put(final Map<String, ?> values) {
    requireNonNull(name, "Model name is required.");
    values.forEach((k, v) -> model.put(k, v));
    return this;
  }

  /**
   * @return View's model.
   */
  public Map<String, ?> model() {
    return model;
  }

  /**
   * @return The name of the view engine or empty string for default view engine.
   */
  public String engine() {
    return engine;
  }

  /**
   * Set the view engine to use.
   *
   * @param engine Set the view engine to use.
   * @return This view.
   */
  public View engine(final String engine) {
    this.engine = requireNonNull(engine, "A view engine is required.");
    return this;
  }

  @Override
  public String toString() {
    return name + ": " + model;
  }

  /**
   * Creates a new {@link View}.
   *
   * @param view View's name.
   * @return A new viewable.
   */
  public static View of(final String view) {
    return new View(view);
  }

  /**
   * Creates a new {@link View}.
   *
   * @param view View's name.
   * @param name Model's name.
   * @param value Model's value.
   * @return A new viewable.
   */
  public static View of(final String view, final String name, final Object value) {
    return new View(view).put(name, value);
  }

}

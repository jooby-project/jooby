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

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * Special result that hold view name and model. It will be processed by a {@link View.Engine}.
 *
 * @author edgar
 * @since 0.1.0
 */
public class View extends Result {

  /**
   * Special body serializer for dealing with {@link View}.
   *
   * @author edgar
   * @since 0.1.0
   */
  public interface Engine extends Renderer {

    List<MediaType> HTML = ImmutableList.of(MediaType.html);

    @Override
    default void render(final Object value, final Renderer.Context ctx) throws Exception {
      if (value instanceof View) {
        View view = (View) value;
        try {
          ctx.type(MediaType.html);
          render(view, ctx);
        } catch (FileNotFoundException ex) {
          LoggerFactory.getLogger(getClass()).debug("Template not found: " + view.name(), ex);
        }
      }
    }

    /**
     * Render a view.
     *
     * @param viewable View to render.
     * @param ctx A rendering context.
     * @throws Exception If view rendering fails.
     */
    void render(final View viewable, final Renderer.Context ctx) throws Exception;

  }

  /** View's name. */
  private final String name;

  /** View's model. */
  private final Map<String, Object> model = new HashMap<>();

  /**
   * Creates a new {@link View}.
   *
   * @param name View's name.
   */
  protected View(final String name) {
    this.name = requireNonNull(name, "View name is required.");
    type(MediaType.html);
    super.set(this);
  }

  /**
   * @return View's name.
   */
  public String name() {
    return name;
  }

  /**
   * Set a model attribute and override existing attribute.
   *
   * @param name Attribute's name.
   * @param value Attribute's value.
   * @return This view.
   */
  public View put(final String name, final Object value) {
    requireNonNull(name, "Model name is required.");
    model.put(name, value);
    return this;
  }

  /**
   * Set model attributes and override existing values.
   *
   * @param values Attribute's value.
   * @return This view.
   */
  public View put(final Map<String, ?> values) {
    values.forEach((k, v) -> model.put(k, v));
    return this;
  }

  /**
   * @return View's model.
   */
  public Map<String, ?> model() {
    return model;
  }

  @Override
  public Result set(final Object content) {
    throw new UnsupportedOperationException("Not allowed in views, use one of the put methods.");
  }

  @Override
  public String toString() {
    return name + ": " + model;
  }

}

/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Used by template engines to renderer views.
 *
 * @since 2.0.0
 * @author edgar
 */
public class ModelAndView {

  /** View name. */
  public final String view;

  /** View data. */
  public final Map<String, Object> model;

  /**
   * Creates a new model and view.
   *
   * @param view View name.
   * @param model View model.
   */
  public ModelAndView(@Nonnull String view, @Nonnull Map<String, Object> model) {
    this.view = view;
    this.model = model;
  }

  /**
   * Creates a new model and view.
   *
   * @param view View name.
   */
  public ModelAndView(@Nonnull String view) {
    this(view, new HashMap<>());
  }

  /**
   * Put a model attribute.
   *
   * @param name Name.
   * @param value Value.
   * @return This model and view.
   */
  public ModelAndView put(@Nonnull String name, Object value) {
    model.put(name, value);
    return this;
  }

  /**
   * Copy all the attributes into the model.
   *
   * @param attributes Attributes.
   * @return This model and view.
   */
  public ModelAndView put(@Nonnull Map<String, Object> attributes) {
    model.putAll(attributes);
    return this;
  }

  @Override public String toString() {
    return view;
  }
}

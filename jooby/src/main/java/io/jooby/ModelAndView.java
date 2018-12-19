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

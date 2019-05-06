/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handlebars;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;

import java.util.HashMap;
import java.util.Map;

class HbsTemplateEngine implements TemplateEngine {

  private Handlebars handlebars;

  public HbsTemplateEngine(Handlebars handlebars) {
    this.handlebars = handlebars;
  }

  @Override public String apply(Context ctx, ModelAndView modelAndView) throws Exception {
    Template template = handlebars.compile(modelAndView.view);
    Map<String, Object> model = new HashMap<>(ctx.getAttributes());
    model.putAll(modelAndView.model);
    return template.apply(model);
  }
}

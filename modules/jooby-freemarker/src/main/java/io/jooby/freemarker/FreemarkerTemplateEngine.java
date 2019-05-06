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
package io.jooby.freemarker;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.jooby.Context;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

class FreemarkerTemplateEngine implements TemplateEngine {

  private final Configuration freemarker;

  public FreemarkerTemplateEngine(Configuration freemarker) {
    this.freemarker = freemarker;
  }

  @Override public String apply(Context ctx, ModelAndView modelAndView) throws Exception {
    Template template = freemarker.getTemplate(modelAndView.view);
    StringWriter writer = new StringWriter();
    Map<String, Object> model = new HashMap<>(ctx.getAttributes());
    model.putAll(modelAndView.model);
    template.process(model, writer);
    return writer.toString();
  }
}

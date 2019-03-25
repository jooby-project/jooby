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

import com.github.jknack.handlebars.Decorator;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.HighConcurrencyTemplateCache;
import com.github.jknack.handlebars.cache.NullTemplateCache;
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.jooby.Context;
import io.jooby.Env;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class Hbs implements TemplateEngine {

  public static class Builder {

    private Handlebars handlebars;

    private TemplateLoader loader;

    private TemplateCache cache;

    public Builder() {
      handlebars = new Handlebars();
      handlebars.setCharset(StandardCharsets.UTF_8);
    }

    public Builder cache(TemplateCache cache) {
      this.cache = cache;
      return this;
    }

    public Builder loader(TemplateLoader loader) {
      this.loader = loader;
      return this;
    }

    public <H> Builder registerHelper(String name, Helper<H> helper) {
      handlebars.registerHelper(name, helper);
      return this;
    }

    public <H> Builder registerHelperMissing(Helper<H> helper) {
      handlebars.registerHelperMissing(helper);
      return this;
    }

    public Builder registerHelpers(Object helperSource) {
      handlebars.registerHelpers(helperSource);
      return this;
    }

    public Builder registerHelpers(Class<?> helperSource) {
      handlebars.registerHelpers(helperSource);
      return this;
    }

    public Builder registerHelpers(URI location) throws Exception {
      handlebars.registerHelpers(location);
      return this;
    }

    public Builder registerHelpers(File input) throws Exception {
      handlebars.registerHelpers(input);
      return this;
    }

    public Builder registerHelpers(String filename, Reader source)
        throws Exception {
      handlebars.registerHelpers(filename, source);
      return this;
    }

    public Builder registerHelpers(String filename, InputStream source)
        throws Exception {
      handlebars.registerHelpers(filename, source);
      return this;
    }

    public Builder registerHelpers(String filename, String source)
        throws IOException {
      handlebars.registerHelpers(filename, source);
      return this;
    }

    public Builder registerDecorator(String name, Decorator decorator) {
      handlebars.registerDecorator(name, decorator);
      return this;
    }

    public Builder charset(Charset charset) {
      handlebars.setCharset(charset);
      return this;
    }

    public Hbs build() {
      return build(Env.empty("dev"));
    }

    public Hbs build(Env env) {
      handlebars.with(ofNullable(loader).orElseGet(this::defaultTemplateLoader));

      TemplateCache cache = ofNullable(this.cache).orElseGet(() ->
          env.matches("dev", "test") ?
              NullTemplateCache.INSTANCE :
              new HighConcurrencyTemplateCache()
      );
      handlebars.with(cache);

      Hbs result = new Hbs(handlebars);
      this.loader = null;
      this.handlebars = null;
      this.cache = null;
      return result;
    }

    private TemplateLoader defaultTemplateLoader() {
      return new ClassPathTemplateLoader("/views", "");
    }
  }

  private Handlebars handlebars;

  public Hbs(Handlebars handlebars) {
    this.handlebars = handlebars;
  }

  @Override public String apply(Context ctx, ModelAndView modelAndView) throws Exception {
    Template template = handlebars.compile(modelAndView.view);
    Map<String, Object> model = new HashMap<>(ctx.getAttributes().toMap());
    model.putAll(modelAndView.model);
    return template.apply(model);
  }

  public static Hbs.Builder builder() {
    return new Hbs.Builder();
  }
}

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
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.jooby.Context;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.ModelAndView;
import io.jooby.TemplateEngine;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Optional.ofNullable;

public class Hbs implements Extension {

  public static class Builder {

    private Handlebars handlebars;

    private TemplateLoader loader;

    private TemplateCache cache;

    private String templatePath = "views";

    public Builder() {
      handlebars = new Handlebars();
      handlebars.setCharset(StandardCharsets.UTF_8);
    }

    public @Nonnull Builder setTemplateCache(@Nonnull TemplateCache cache) {
      this.cache = cache;
      return this;
    }

    public @Nonnull Builder setTemplatePath(@Nonnull String templatePath) {
      this.templatePath = templatePath.startsWith("/") ? templatePath.substring(1) : templatePath;
      return this;
    }

    public @Nonnull Builder setTemplateLoader(@Nonnull TemplateLoader loader) {
      this.loader = loader;
      return this;
    }

    public @Nonnull <H> Builder registerHelper(@Nonnull String name, @Nonnull Helper<H> helper) {
      handlebars.registerHelper(name, helper);
      return this;
    }

    public @Nonnull <H> Builder registerHelperMissing(@Nonnull Helper<H> helper) {
      handlebars.registerHelperMissing(helper);
      return this;
    }

    public @Nonnull Builder registerHelpers(@Nonnull Object helperSource) {
      handlebars.registerHelpers(helperSource);
      return this;
    }

    public @Nonnull Builder registerHelpers(@Nonnull Class<?> helperSource) {
      handlebars.registerHelpers(helperSource);
      return this;
    }

    public @Nonnull Builder registerHelpers(@Nonnull URI location) throws Exception {
      handlebars.registerHelpers(location);
      return this;
    }

    public @Nonnull Builder registerHelpers(@Nonnull File input) throws Exception {
      handlebars.registerHelpers(input);
      return this;
    }

    public @Nonnull Builder registerHelpers(@Nonnull String filename, @Nonnull Reader source)
        throws Exception {
      handlebars.registerHelpers(filename, source);
      return this;
    }

    public @Nonnull Builder registerHelpers(@Nonnull String filename, @Nonnull InputStream source)
        throws Exception {
      handlebars.registerHelpers(filename, source);
      return this;
    }

    public @Nonnull Builder registerHelpers(@Nonnull String filename, @Nonnull String source)
        throws IOException {
      handlebars.registerHelpers(filename, source);
      return this;
    }

    public @Nonnull Builder registerDecorator(@Nonnull String name, @Nonnull Decorator decorator) {
      handlebars.registerDecorator(name, decorator);
      return this;
    }

    public @Nonnull Handlebars build(@Nonnull Environment env) {
      if (loader == null) {
        loader = defaultTemplateLoader(env, templatePath);
      }
      handlebars.with(loader);

      if (cache == null) {
        cache = env.isActive("dev", "test")
            ? NullTemplateCache.INSTANCE
            : new HighConcurrencyTemplateCache();
      }
      handlebars.with(cache);

      this.loader = null;
      this.cache = null;
      return handlebars;
    }

    private static TemplateLoader defaultTemplateLoader(Environment env, String templatePath) {
      Path dir = Paths.get(System.getProperty("user.dir"), templatePath);
      if (Files.exists(dir)) {
        return new FileTemplateLoader(dir.toFile(), "");
      }
      ClassLoader classLoader = env.getClassLoader();
      return new ClassPathTemplateLoader(templatePath, "") {
        @Override protected URL getResource(String location) {
          return classLoader.getResource(location);
        }
      };
    }
  }

  private Handlebars handlebars;

  public Hbs(@Nonnull Handlebars handlebars) {
    this.handlebars = handlebars;
  }

  public Hbs() {
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    if (handlebars == null) {
      handlebars = create().build(application.getEnvironment());
    }
    application.renderer(MediaType.html, new HbsTemplateEngine(handlebars));
  }

  public static Hbs.Builder create() {
    return new Hbs.Builder();
  }
}

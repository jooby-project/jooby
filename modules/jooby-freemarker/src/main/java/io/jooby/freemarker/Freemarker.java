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

import com.typesafe.config.Config;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.OutputFormat;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.Sneaky;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import static io.jooby.Sneaky.throwingConsumer;

public class Freemarker implements Extension {

  public static class Builder {

    private TemplateLoader templateLoader;

    private Properties settings = new Properties();

    private OutputFormat outputFormat = HTMLOutputFormat.INSTANCE;

    private String templatePath;

    public @Nonnull Builder setTemplateLoader(@Nonnull TemplateLoader loader) {
      this.templateLoader = loader;
      return this;
    }

    public @Nonnull Builder setSetting(@Nonnull String name, @Nonnull String value) {
      this.settings.put(name, value);
      return this;
    }

    public @Nonnull Builder setOutputFormat(@Nonnull OutputFormat outputFormat) {
      this.outputFormat = outputFormat;
      return this;
    }

    public @Nonnull Builder setTemplatePath(@Nonnull String templatePath) {
      if (templatePath.startsWith("/")) {
        this.templatePath = templatePath.substring(1);
      } else {
        this.templatePath = templatePath;
      }
      return this;
    }

    public @Nonnull Configuration build(@Nonnull Environment env) {
      try {
        Configuration freemarker = new Configuration(
            Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        freemarker.setOutputFormat(outputFormat);

        /** Settings: */
        Config conf = env.getConfig();
        if (conf.hasPath("freemarker")) {
          conf.getConfig("freemarker").root().unwrapped()
              .forEach((k, v) -> settings.put(k, v.toString()));
        }
        String templatePath = Optional.ofNullable((String) settings.remove("templatePath"))
            .orElse(Optional.ofNullable(this.templatePath).orElse("views"));

        settings.putIfAbsent("defaultEncoding", "UTF-8");
        /** Cache storage: */
        String defaultCacheStorage = env.isActive("dev", "test")
            ? "freemarker.cache.NullCacheStorage"
            : "soft";
        settings.putIfAbsent(Configuration.CACHE_STORAGE_KEY_CAMEL_CASE, defaultCacheStorage);

        freemarker.setSettings(settings);

        /** Template path: */
        setTemplatePath(templatePath);

        /** Template loader: */
        if (templateLoader == null) {
          templateLoader = defaultTemplateLoader(env);
        }
        freemarker.setTemplateLoader(templateLoader);

        /** Object wrapper: */
        DefaultObjectWrapperBuilder dowb = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_22);
        dowb.setExposeFields(true);
        freemarker.setObjectWrapper(dowb.build());

        // clear
        this.templateLoader = null;
        this.settings.clear();
        this.settings = null;
        return freemarker;
      } catch (TemplateException x) {
        throw Sneaky.propagate(x);
      }
    }

    private TemplateLoader defaultTemplateLoader(Environment env) {
      try {
        Path templateDir = Paths.get(System.getProperty("user.dir"), templatePath);
        if (Files.exists(templateDir)) {
          return new FileTemplateLoader(templateDir.toFile());
        }
        return new ClassTemplateLoader(env.getClassLoader(), "/" + templatePath);
      } catch (Exception x) {
        throw Sneaky.propagate(x);
      }
    }
  }

  private Configuration freemarker;

  public Freemarker(@Nonnull Configuration freemarker) {
    this.freemarker = freemarker;
  }

  public Freemarker() {
  }

  @Override public void install(@Nonnull Jooby application) {
    if (freemarker == null) {
      freemarker = create().build(application.getEnvironment());
    }
    application.renderer(MediaType.html, new FreemarkerTemplateEngine(freemarker));
  }

  public static Freemarker.Builder create() {
    return new Freemarker.Builder();
  }
}

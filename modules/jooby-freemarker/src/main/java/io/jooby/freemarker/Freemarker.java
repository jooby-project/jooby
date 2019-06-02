/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
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
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateException;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.ServiceRegistry;
import io.jooby.Sneaky;
import io.jooby.TemplateEngine;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static io.jooby.TemplateEngine.TEMPLATE_PATH;
import static io.jooby.TemplateEngine.normalizePath;

public class Freemarker implements Extension {

  public static class Builder {

    private TemplateLoader templateLoader;

    private Properties settings = new Properties();

    private OutputFormat outputFormat = HTMLOutputFormat.INSTANCE;

    private String templatesPath = TemplateEngine.PATH;

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

    public @Nonnull Builder setTemplatesPath(@Nonnull String templatesPath) {
        this.templatesPath = templatesPath;
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
        String templatesPath = normalizePath(env.getProperty(TEMPLATE_PATH, this.templatesPath));

        settings.putIfAbsent("defaultEncoding", "UTF-8");
        /** Cache storage: */
        String defaultCacheStorage = env.isActive("dev", "test")
            ? "freemarker.cache.NullCacheStorage"
            : "soft";
        settings.putIfAbsent(Configuration.CACHE_STORAGE_KEY_CAMEL_CASE, defaultCacheStorage);

        freemarker.setSettings(settings);

        /** Template loader: */
        if (templateLoader == null) {
          templateLoader = defaultTemplateLoader(env, templatesPath);
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

    private TemplateLoader defaultTemplateLoader(Environment env, String templatesPath) {
      try {
        Path templateDir = Paths.get(System.getProperty("user.dir"), templatesPath);
        if (Files.exists(templateDir)) {
          return new FileTemplateLoader(templateDir.toFile());
        }
        return new ClassTemplateLoader(env.getClassLoader(), "/" + templatesPath);
      } catch (Exception x) {
        throw Sneaky.propagate(x);
      }
    }
  }

  private Configuration freemarker;

  private String templatesPath;

  public Freemarker(@Nonnull Configuration freemarker) {
    this.freemarker = freemarker;
  }

  public Freemarker(@Nonnull String templatesPath) {
    this.templatesPath = templatesPath;
  }

  public Freemarker() {
    this(TemplateEngine.PATH);
  }

  @Override public void install(@Nonnull Jooby application) {
    if (freemarker == null) {
      freemarker = create().setTemplatesPath(templatesPath).build(application.getEnvironment());
    }
    application.renderer(MediaType.html, new FreemarkerTemplateEngine(freemarker));

    ServiceRegistry services = application.getServices();
    services.put(Configuration.class, freemarker);
  }

  public static Freemarker.Builder create() {
    return new Freemarker.Builder();
  }
}

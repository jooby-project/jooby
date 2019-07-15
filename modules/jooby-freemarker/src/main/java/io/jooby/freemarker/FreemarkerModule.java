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
import io.jooby.SneakyThrows;
import io.jooby.TemplateEngine;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static io.jooby.TemplateEngine.TEMPLATE_PATH;
import static io.jooby.TemplateEngine.normalizePath;

/**
 * Freemarker module: https://jooby.io/modules/freemarker.
 *
 * Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new FreemarkerModule());
 *
 *   get("/", ctx -> {
 *     User user = ...;
 *     return new ModelAndView("index.ftl")
 *         .put("user", user);
 *   });
 * }
 * }</pre>
 *
 * The template engine looks for a file-system directory: <code>views</code> in the current
 * user directory. If the directory doesn't exist, it looks for the same directory in the project
 * classpath.
 *
 * You can specify a different template location:
 *
 * <pre>{@code
 * {
 *
 *    install(new FreemarkerModule("mypath"));
 *
 * }
 * }</pre>
 *
 * The <code>mypath</code> location works in the same way: file-system or fallback to classpath.
 *
 * Direct access to {@link Configuration} is available via require call:
 *
 * <pre>{@code
 * {
 *
 *   Configuration configuration = require(Configuration.class);
 *
 * }
 * }</pre>
 *
 * Complete documentation is available at: https://jooby.io/modules/freemarker.
 *
 * @author edgar
 * @since 2.0.0
 */
public class FreemarkerModule implements Extension {

  /**
   * Utility class for creating {@link Configuration} instances.
   */
  public static class Builder {

    private TemplateLoader templateLoader;

    private Properties settings = new Properties();

    private OutputFormat outputFormat = HTMLOutputFormat.INSTANCE;

    private String templatesPath = TemplateEngine.PATH;

    /**
     * Template loader to use.
     *
     * @param loader Template loader to use.
     * @return This builder.
     */
    public @Nonnull Builder setTemplateLoader(@Nonnull TemplateLoader loader) {
      this.templateLoader = loader;
      return this;
    }

    /**
     * Set a freemarker option/setting.
     *
     * @param name Option name.
     * @param value Optiona value.
     * @return This builder.
     */
    public @Nonnull Builder setSetting(@Nonnull String name, @Nonnull String value) {
      this.settings.put(name, value);
      return this;
    }

    /**
     * Set output format.
     *
     * @param outputFormat Output format.
     * @return This builder.
     */
    public @Nonnull Builder setOutputFormat(@Nonnull OutputFormat outputFormat) {
      this.outputFormat = outputFormat;
      return this;
    }

    /**
     * Template path.
     *
     * @param templatesPath Set template path.
     * @return This builder.
     */
    public @Nonnull Builder setTemplatesPath(@Nonnull String templatesPath) {
        this.templatesPath = templatesPath;
      return this;
    }

    /**
     * Build method for creating a freemarker instance.
     *
     * @param env Application environment.
     * @return A new freemarker instance.
     */
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
        throw SneakyThrows.propagate(x);
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
        throw SneakyThrows.propagate(x);
      }
    }
  }

  private Configuration freemarker;

  private String templatesPath;

  /**
   * Creates a new freemarker module using a the given freemarker instance.
   *
   * @param freemarker Freemarker to use.
   */
  public FreemarkerModule(@Nonnull Configuration freemarker) {
    this.freemarker = freemarker;
  }

  /**
   * Freemarker module which look at the given path. It first look at the file-system or fallback
   * to classpath.
   *
   * @param templatesPath Template path.
   */
  public FreemarkerModule(@Nonnull String templatesPath) {
    this.templatesPath = templatesPath;
  }

  /**
   * Creates a new freemarker module using the default template path: <code>views</code>.
   */
  public FreemarkerModule() {
    this(TemplateEngine.PATH);
  }

  @Override public void install(@Nonnull Jooby application) {
    if (freemarker == null) {
      freemarker = create().setTemplatesPath(templatesPath).build(application.getEnvironment());
    }
    application.encoder(MediaType.html, new FreemarkerTemplateEngine(freemarker));

    ServiceRegistry services = application.getServices();
    services.put(Configuration.class, freemarker);
  }

  /**
   * Creates a new freemarker builder.
   *
   * @return A builder.
   */
  public static @Nonnull FreemarkerModule.Builder create() {
    return new FreemarkerModule.Builder();
  }
}

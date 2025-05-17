/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.freemarker;

import static io.jooby.TemplateEngine.TEMPLATE_PATH;
import static io.jooby.TemplateEngine.normalizePath;
import static java.util.Arrays.asList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import edu.umd.cs.findbugs.annotations.NonNull;
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
import io.jooby.SneakyThrows;
import io.jooby.TemplateEngine;

/**
 * Freemarker module: https://jooby.io/modules/freemarker.
 *
 * <p>Usage:
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
 * The template engine looks for a file-system directory: <code>views</code> in the current user
 * directory. If the directory doesn't exist, it looks for the same directory in the project
 * classpath.
 *
 * <p>Template engine supports the following file extensions: <code>.ftl</code>, <code>.ftl.html
 * </code> and <code>.html</code>.
 *
 * <p>You can specify a different template location:
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
 * <p>Direct access to {@link Configuration} is available via require call:
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

  /** Utility class for creating {@link Configuration} instances. */
  public static class Builder {

    private TemplateLoader templateLoader;

    private Properties settings = new Properties();

    private OutputFormat outputFormat = HTMLOutputFormat.INSTANCE;

    private String templatesPathString = TemplateEngine.PATH;

    private Path templatesPath;

    /**
     * Template loader to use.
     *
     * @param loader Template loader to use.
     * @return This builder.
     */
    public @NonNull Builder setTemplateLoader(@NonNull TemplateLoader loader) {
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
    public @NonNull Builder setSetting(@NonNull String name, @NonNull String value) {
      this.settings.put(name, value);
      return this;
    }

    /**
     * Set output format.
     *
     * @param outputFormat Output format.
     * @return This builder.
     */
    public @NonNull Builder setOutputFormat(@NonNull OutputFormat outputFormat) {
      this.outputFormat = outputFormat;
      return this;
    }

    /**
     * Template path.
     *
     * @param templatesPath Set template path.
     * @return This builder.
     */
    public @NonNull Builder setTemplatesPath(@NonNull String templatesPath) {
      this.templatesPathString = templatesPath;
      return this;
    }

    /**
     * Template path.
     *
     * @param templatesPath Set template path.
     * @return This builder.
     */
    public @NonNull Builder setTemplatesPath(@NonNull Path templatesPath) {
      this.templatesPath = templatesPath;
      return this;
    }

    /**
     * Build method for creating a freemarker instance.
     *
     * @param env Application environment.
     * @return A new freemarker instance.
     */
    public @NonNull Configuration build(@NonNull Environment env) {
      try {
        var freemarker = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        freemarker.setOutputFormat(outputFormat);

        /** Settings: */
        var conf = env.getConfig();
        if (conf.hasPath("freemarker")) {
          conf.getConfig("freemarker")
              .root()
              .unwrapped()
              .forEach((k, v) -> settings.put(k, v.toString()));
        }

        settings.putIfAbsent("defaultEncoding", "UTF-8");
        /** Cache storage: */
        var defaultCacheStorage =
            env.isActive("dev", "test") ? "freemarker.cache.NullCacheStorage" : "soft";
        settings.putIfAbsent(Configuration.CACHE_STORAGE_KEY_CAMEL_CASE, defaultCacheStorage);

        freemarker.setSettings(settings);

        /** Template loader: */
        if (templateLoader == null) {
          var templatesPathString =
              normalizePath(
                  env.getProperty(
                      TEMPLATE_PATH,
                      Optional.ofNullable(this.templatesPathString).orElse(TemplateEngine.PATH)));
          templateLoader = defaultTemplateLoader(env, templatesPathString, templatesPath);
        }
        freemarker.setTemplateLoader(templateLoader);

        /** Object wrapper: */
        var dowb = new DefaultObjectWrapperBuilder(Configuration.VERSION_2_3_22);
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

    private TemplateLoader defaultTemplateLoader(
        Environment env, String templatesPathString, Path templatesPath) {
      try {
        var templateDir =
            Optional.ofNullable(templatesPath)
                .orElse(Paths.get(System.getProperty("user.dir"), templatesPathString));
        if (Files.exists(templateDir)) {
          return new FileTemplateLoader(templateDir.toFile());
        }
        return new ClassTemplateLoader(env.getClassLoader(), "/" + templatesPathString);
      } catch (Exception x) {
        throw SneakyThrows.propagate(x);
      }
    }
  }

  private static final List<String> EXT = asList(".ftl", ".ftl.html", ".html");

  private Configuration freemarker;

  private String templatesPathString;

  private Path templatesPath;

  /**
   * Creates a new freemarker module using a the given freemarker instance.
   *
   * @param freemarker Freemarker to use.
   */
  public FreemarkerModule(@NonNull Configuration freemarker) {
    this.freemarker = freemarker;
  }

  /**
   * Freemarker module which look at the given path. It first look at the file-system or fallback to
   * classpath.
   *
   * @param templatesPath Template path.
   */
  public FreemarkerModule(@NonNull String templatesPath) {
    this.templatesPathString = templatesPath;
  }

  /**
   * Freemarker module that looks at the given path.
   *
   * @param templatesPath Template path.
   */
  public FreemarkerModule(@NonNull Path templatesPath) {
    this.templatesPath = templatesPath;
  }

  /** Creates a new freemarker module using the default template path: <code>views</code>. */
  public FreemarkerModule() {
    this(TemplateEngine.PATH);
  }

  @Override
  public void install(@NonNull Jooby application) {
    if (freemarker == null) {
      freemarker =
          create()
              .setTemplatesPath(templatesPathString)
              .setTemplatesPath(templatesPath)
              .build(application.getEnvironment());
    }
    application.encoder(new FreemarkerTemplateEngine(freemarker, EXT));

    var services = application.getServices();
    services.put(Configuration.class, freemarker);
  }

  /**
   * Creates a new freemarker builder.
   *
   * @return A builder.
   */
  public static @NonNull FreemarkerModule.Builder create() {
    return new FreemarkerModule.Builder();
  }
}

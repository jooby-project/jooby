/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.pebble;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.attributes.methodaccess.BlacklistMethodAccessValidator;
import com.mitchellbosecke.pebble.attributes.methodaccess.NoOpMethodAccessValidator;
import com.mitchellbosecke.pebble.cache.CacheKey;
import com.mitchellbosecke.pebble.cache.PebbleCache;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.loader.DelegatingLoader;
import com.mitchellbosecke.pebble.loader.FileLoader;
import com.mitchellbosecke.pebble.loader.Loader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import com.typesafe.config.Config;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.TemplateEngine;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;

import static java.util.Arrays.asList;

/**
 * Pebble module: https://jooby.io/modules/pebble.
 * <p>
 * Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new PebbleModule());
 *
 *   get("/", ctx -> {
 *     User user = ...;
 *     return new ModelAndView("index.peb")
 *         .put("user", user);
 *   });
 * }
 * }</pre>
 * <p>
 * The template engine looks for a file-system directory: <code>views</code> in the current
 * user directory. If the directory doesn't exist, it looks for the same directory in the project
 * classpath.
 * <p>
 * You can specify a different template location:
 *
 * <pre>{@code
 * {
 *
 *    install(new PebbleModule("mypath"));
 *
 * }
 * }</pre>
 * <p>
 * The <code>mypath</code> location works in the same way: file-system or fallback to classpath.
 * <p>
 * Template engine supports the following file extensions: <code>.peb</code>, <code>.pebble</code> and <code>.html</code>.
 * <p>
 * Direct access to {@link PebbleEngine.Builder} is available via require call:
 *
 * <pre>{@code
 * {
 *
 *   PebbleEngine.Builder builder = require(PebbleEngine.Builder.class);
 *
 * }
 * }</pre>
 * <p>
 * Complete documentation is available at: https://jooby.io/modules/pebble.
 *
 * @author sojin
 * @since 2.0.0
 */
public class PebbleModule implements Extension {

  /**
   * Utility class for creating {@link PebbleModule} instances.
   */
  public static class Builder {

    private Loader<?> loader;
    private ExecutorService executorService;
    private Locale defaultLocale;
    private PebbleCache<CacheKey, Object> tagCache;
    private PebbleCache<Object, PebbleTemplate> templateCache;

    private String templatesPath = TemplateEngine.PATH;

    /**
     * Set template cache.
     *
     * @param cache Template cache.
     * @return This builder.
     */
    public @Nonnull
    Builder setTemplateCache(@Nonnull PebbleCache<Object, PebbleTemplate> cache) {
      this.templateCache = cache;
      return this;
    }

    /**
     * Set tab cache.
     *
     * @param tagCache Tag cache.
     * @return This builder.
     */
    public @Nonnull
    Builder setTagCache(@Nonnull PebbleCache<CacheKey, Object> tagCache) {
      this.tagCache = tagCache;
      return this;
    }

    /**
     * Template path.
     *
     * @param templatesPath Set template path.
     * @return This builder.
     */
    public @Nonnull
    Builder setTemplatesPath(@Nonnull String templatesPath) {
      this.templatesPath = templatesPath;
      return this;
    }

    /**
     * set ExecutorService.
     *
     * @param executorService Set ExecutorService.
     * @return This builder.
     */
    public @Nonnull
    Builder setExecutorService(@Nonnull ExecutorService executorService) {
      this.executorService = executorService;
      return this;
    }

    /**
     * Set default locale.
     *
     * @param defaultLocale Locale.
     * @return This builder.
     */
    public @Nonnull
    Builder setDefaultLocale(@Nonnull Locale defaultLocale) {
      this.defaultLocale = defaultLocale;
      return this;
    }

    /**
     * Template loader to use.
     *
     * @param loader Template loader to use.
     * @return This builder.
     */
    public @Nonnull
    Builder setTemplateLoader(@Nonnull Loader<?> loader) {
      this.loader = loader;
      return this;
    }

    /**
     * Creates a PebbleEngine instance.
     *
     * @param env Application environment.
     * @return A new PebbleEngine instance.
     */
    public @Nonnull
    PebbleEngine.Builder build(@Nonnull Environment env) {

      PebbleEngine.Builder builder = new PebbleEngine.Builder();

      /** Settings: */

      if (env.isActive("dev", "test")) {
        builder.cacheActive(false);
      }
      Config conf = env.getConfig();
      if (conf.hasPath("pebble.cacheActive")) {
        builder.cacheActive(conf.getBoolean("pebble.cacheActive"));
      }
      if (conf.hasPath("pebble.strictVariables")) {
        builder.strictVariables(conf.getBoolean("pebble.strictVariables"));
      }
      if (conf.hasPath("pebble.allowUnsafeMethods")) {
        if (conf.getBoolean("pebble.allowUnsafeMethods")) {
          builder.methodAccessValidator(new NoOpMethodAccessValidator());
        } else {
          builder.methodAccessValidator(new BlacklistMethodAccessValidator());
        }
      }
      if (conf.hasPath("pebble.literalDecimalTreatedAsInteger")) {
        builder.literalDecimalTreatedAsInteger(
            conf.getBoolean("pebble.literalDecimalTreatedAsInteger"));
      }
      if (conf.hasPath("pebble.greedyMatchMethod")) {
        builder.greedyMatchMethod(conf.getBoolean("pebble.greedyMatchMethod"));
      }
      if (tagCache != null) {
        builder.tagCache(tagCache);
      }
      if (templateCache != null) {
        builder.templateCache(templateCache);
      }
      if (loader != null) {
        builder.loader(loader);
      } else {
        String extension = null;
        if (conf.hasPath("pebble.extension")) {
          extension = conf.getString("pebble.extension");
        }
        builder.loader(getDefaultLoader(templatesPath, extension));
      }
      if (executorService != null) {
        builder.executorService(executorService);
      }
      if (defaultLocale != null) {
        builder.defaultLocale(defaultLocale);
      }

      return builder;
    }

    DelegatingLoader getDefaultLoader(String templatesPath, String extension) {
      List<Loader<?>> loaders = new ArrayList<>();

      ClasspathLoader cLoader = new ClasspathLoader();
      if (templatesPath == null) {
        templatesPath = TemplateEngine.PATH;
      }
      cLoader.setPrefix(stripLeadingSlash(templatesPath));
      cLoader.setSuffix(extension);
      loaders.add(cLoader);

      FileLoader fLoader = new FileLoader();
      Path dir = Paths.get(System.getProperty("user.dir"), templatesPath);
      if (Files.exists(dir)) {
        fLoader.setPrefix(dir.normalize().toString());
        fLoader.setSuffix(extension);
        loaders.add(fLoader);
      }
      return new DelegatingLoader(loaders);
    }
  }

  private static String stripLeadingSlash(String value) {
    if (value == null) {
      return null;
    }
    if (value.startsWith("/")) {
      return value.substring(1);
    }
    return value;
  }

  private static final List<String> EXT = asList(".peb", ".pebble", ".html");

  private PebbleEngine.Builder builder;

  private String templatesPath;

  /**
   * Creates a new pebble module.
   *
   * @param builder PebbleEngine.Builder instance to use.
   */
  public PebbleModule(@Nonnull PebbleEngine.Builder builder) {
    this.builder = builder;
  }

  /**
   * Creates a new PebbleModule module.
   *
   * @param templatesPath Template location to use. First try to file-system or fallback to
   *                      classpath.
   */
  public PebbleModule(@Nonnull String templatesPath) {
    this.templatesPath = templatesPath;
  }

  /**
   * Creates a new PebbleModule module using the default path: <code>views</code>.
   */
  public PebbleModule() {
    this(TemplateEngine.PATH);
  }

  @Override
  public void install(@Nonnull Jooby application) throws Exception {
    if (builder == null) {
      builder = create().setTemplatesPath(templatesPath).build(application.getEnvironment());
    }
    application.encoder(new PebbleTemplateEngine(builder, EXT));

    ServiceRegistry services = application.getServices();
    services.put(PebbleEngine.Builder.class, builder);
  }

  /**
   * Creates a new PebbleModule.Builder.
   *
   * @return A builder.
   */
  public static @Nonnull
  PebbleModule.Builder create() {
    return new PebbleModule.Builder();
  }
}

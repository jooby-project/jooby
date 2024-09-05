/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handlebars;

import static io.jooby.TemplateEngine.TEMPLATE_PATH;
import static io.jooby.TemplateEngine.normalizePath;
import static java.util.Arrays.asList;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.cache.HighConcurrencyTemplateCache;
import com.github.jknack.handlebars.cache.NullTemplateCache;
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.TemplateEngine;
import io.jooby.internal.handlebars.HandlebarsTemplateEngine;

/**
 * Handlebars module: https://jooby.io/modules/handlebars.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new HandlebarsModule());
 *
 *   get("/", ctx -> {
 *     User user = ...;
 *     return new ModelAndView("index.hbs")
 *         .put("user", user);
 *   });
 * }
 * }</pre>
 *
 * The template engine looks for a file-system directory: <code>views</code> in the current user
 * directory. If the directory doesn't exist, it looks for the same directory in the project
 * classpath.
 *
 * <p>You can specify a different template location:
 *
 * <pre>{@code
 * {
 *
 *    install(new HandlebarsModule("mypath"));
 *
 * }
 * }</pre>
 *
 * The <code>mypath</code> location works in the same way: file-system or fallback to classpath.
 *
 * <p>Template engine supports the following file extensions: <code>.ftl</code>, <code>.ftl.html
 * </code> and <code>.html</code>.
 *
 * <p>Direct access to {@link Handlebars} is available via require call:
 *
 * <pre>{@code
 * {
 *
 *   Handlebars hbs = require(Handlebars.class);
 *
 * }
 * }</pre>
 *
 * Complete documentation is available at: https://jooby.io/modules/handlebars.
 *
 * @author edgar
 * @since 2.0.0
 */
public class HandlebarsModule implements Extension {

  /** Utility class for creating {@link Handlebars} instances. */
  public static class Builder {

    private Handlebars handlebars = new Handlebars().setCharset(StandardCharsets.UTF_8);

    private TemplateLoader loader;

    private TemplateCache cache;

    private String templatesPathString = TemplateEngine.PATH;

    private Path templatesPath;

    /**
     * Set template cache.
     *
     * @param cache Template cache.
     * @return This builder.
     */
    public @NonNull Builder setTemplateCache(@NonNull TemplateCache cache) {
      this.cache = cache;
      return this;
    }

    /**
     * Template path.
     *
     * @param templatesPathString Set template path.
     * @return This builder.
     */
    public @NonNull Builder setTemplatesPath(@NonNull String templatesPathString) {
      this.templatesPathString = templatesPathString;
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
     * Template loader to use.
     *
     * @param loader Template loader to use.
     * @return This builder.
     */
    public @NonNull Builder setTemplateLoader(@NonNull TemplateLoader loader) {
      this.loader = loader;
      return this;
    }

    /**
     * Creates a handlebars instance.
     *
     * @param env Application environment.
     * @return A new handlebars instance.
     */
    public @NonNull Handlebars build(@NonNull Environment env) {
      if (loader == null) {
        String templatesPathString =
            normalizePath(
                env.getProperty(
                    TEMPLATE_PATH,
                    Optional.ofNullable(this.templatesPathString).orElse(TemplateEngine.PATH)));
        loader = defaultTemplateLoader(env, templatesPathString, templatesPath);
      }
      handlebars.with(loader);

      if (cache == null) {
        cache =
            env.isActive("dev", "test")
                ? NullTemplateCache.INSTANCE
                : new HighConcurrencyTemplateCache();
      }
      handlebars.with(cache);

      this.loader = null;
      this.cache = null;
      return handlebars;
    }

    private static TemplateLoader defaultTemplateLoader(
        Environment env, String templatePathString, Path templatesPath) {
      Path dir =
          Optional.ofNullable(templatesPath)
              .orElse(Paths.get(System.getProperty("user.dir"), templatePathString));
      if (Files.exists(dir)) {
        return new FileTemplateLoader(dir.toFile(), "");
      }
      ClassLoader classLoader = env.getClassLoader();
      return new ClassPathTemplateLoader(templatePathString, "") {
        @Override
        protected URL getResource(String location) {
          return classLoader.getResource(location);
        }
      };
    }
  }

  private static final List<String> EXT = asList(".hbs", ".hbs.html", ".html");

  private Handlebars handlebars;

  private String templatesPathString;

  private Path templatesPath;

  private final Deque<ValueResolver> resolvers =
      new LinkedList<>(ValueResolver.defaultValueResolvers());

  /**
   * Creates a new handlebars module.
   *
   * @param handlebars Handlebars instance to use.
   */
  public HandlebarsModule(@NonNull Handlebars handlebars) {
    this.handlebars = handlebars;
  }

  /**
   * Creates a new handlebars module.
   *
   * @param templatesPath Template location to use. First try to file-system or fallback to
   *     classpath.
   */
  public HandlebarsModule(@NonNull String templatesPath) {
    this.templatesPathString = templatesPath;
  }

  /**
   * Creates a new handlebars module.
   *
   * @param templatesPath Template location to use. First try to file-system or fallback to
   *     classpath.
   */
  public HandlebarsModule(@NonNull Path templatesPath) {
    this.templatesPath = templatesPath;
  }

  /** Creates a new handlebars module using the default path: <code>views</code>. */
  public HandlebarsModule() {
    this(TemplateEngine.PATH);
  }

  /**
   * Add custom value resolver.
   *
   * @param resolver Value resolver.
   * @return This module.
   */
  public HandlebarsModule with(@NonNull ValueResolver resolver) {
    resolvers.addFirst(resolver);
    return this;
  }

  @Override
  public void install(@NonNull Jooby application) throws Exception {
    if (handlebars == null) {
      handlebars =
          create()
              .setTemplatesPath(templatesPathString)
              .setTemplatesPath(templatesPath)
              .build(application.getEnvironment());
    }
    application.encoder(
        new HandlebarsTemplateEngine(handlebars, resolvers.toArray(new ValueResolver[0]), EXT));

    ServiceRegistry services = application.getServices();
    services.put(Handlebars.class, handlebars);
  }

  /**
   * Creates a new freemarker builder.
   *
   * @return A builder.
   */
  public static @NonNull HandlebarsModule.Builder create() {
    return new HandlebarsModule.Builder();
  }
}

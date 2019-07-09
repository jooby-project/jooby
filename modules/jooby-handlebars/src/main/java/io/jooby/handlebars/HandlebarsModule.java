/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handlebars;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.cache.HighConcurrencyTemplateCache;
import com.github.jknack.handlebars.cache.NullTemplateCache;
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.ServiceRegistry;
import io.jooby.TemplateEngine;

import javax.annotation.Nonnull;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.jooby.TemplateEngine.TEMPLATE_PATH;
import static io.jooby.TemplateEngine.normalizePath;

/**
 * Handlebars module: https://jooby.io/modules/handlebars.
 *
 * Usage:
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
 * The template engine looks for a file-system directory: <code>views</code> in the current
 * user directory. If the directory doesn't exist, it looks for the same directory in the project
 * classpath.
 *
 * You can specify a different template location:
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
 * Direct access to {@link Handlebars} is available via require call:
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

  /**
   * Utility class for creating {@link Handlebars} instances.
   */
  public static class Builder {

    private Handlebars handlebars = new Handlebars()
        .setCharset(StandardCharsets.UTF_8);

    private TemplateLoader loader;

    private TemplateCache cache;

    private String templatesPath = TemplateEngine.PATH;

    /**
     * Set template cache.
     *
     * @param cache Template cache.
     * @return This builder.
     */
    public @Nonnull Builder setTemplateCache(@Nonnull TemplateCache cache) {
      this.cache = cache;
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
     * Template loader to use.
     *
     * @param loader Template loader to use.
     * @return This builder.
     */
    public @Nonnull Builder setTemplateLoader(@Nonnull TemplateLoader loader) {
      this.loader = loader;
      return this;
    }

    /**
     * Creates a handlebars instance.
     *
     * @param env Application environment.
     * @return A new handlebars instance.
     */
    public @Nonnull Handlebars build(@Nonnull Environment env) {
      if (loader == null) {
        String templatesPath = normalizePath(env.getProperty(TEMPLATE_PATH, this.templatesPath));
        loader = defaultTemplateLoader(env, templatesPath);
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

  private String templatesPath;

  /**
   * Creates a new handlebars module.
   *
   * @param handlebars Handlebars instance to use.
   */
  public HandlebarsModule(@Nonnull Handlebars handlebars) {
    this.handlebars = handlebars;
  }

  /**
   * Creates a new handlebars module.
   *
   * @param templatesPath Template location to use. First try to file-system or fallback to
   *     classpath.
   */
  public HandlebarsModule(@Nonnull String templatesPath) {
    this.templatesPath = templatesPath;
  }

  /**
   * Creates a new handlebars module using the default path: <code>views</code>.
   */
  public HandlebarsModule() {
    this(TemplateEngine.PATH);
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    if (handlebars == null) {
      handlebars = create().setTemplatesPath(templatesPath).build(application.getEnvironment());
    }
    application.renderer(MediaType.html, new HbsTemplateEngine(handlebars));

    ServiceRegistry services = application.getServices();
    services.put(Handlebars.class, handlebars);
  }

  /**
   * Creates a new freemarker builder.
   *
   * @return A builder.
   */
  public static @Nonnull HandlebarsModule.Builder create() {
    return new HandlebarsModule.Builder();
  }
}

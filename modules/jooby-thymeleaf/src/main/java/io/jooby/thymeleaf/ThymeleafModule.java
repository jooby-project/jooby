/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.thymeleaf;

import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.internal.thymeleaf.ThymeleafTemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.cache.ICacheManager;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static io.jooby.TemplateEngine.TEMPLATE_PATH;
import static io.jooby.TemplateEngine.normalizePath;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;

/**
 * Thymeleaf module: https://jooby.io/modules/thymeleaf.
 *
 * Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new ThymeleafModule());
 *
 *   get("/", ctx -> {
 *     User user = ...;
 *     return new ModelAndView("index.html")
 *         .put("user", user);
 *   });
 * }
 * }</pre>
 *
 * The template engine looks for a file-system directory: <code>views</code> in the current
 * user directory. If the directory doesn't exist, it looks for the same directory in the project
 * classpath.
 *
 * Template engine supports the following file extensions: <code>.thl</code>,
 * <code>.thl.html</code> and <code>.html</code>.
 *
 * You can specify a different template location:
 *
 * <pre>{@code
 * {
 *
 *    install(new ThymeleafModule("mypath"));
 *
 * }
 * }</pre>
 *
 * The <code>mypath</code> location works in the same way: file-system or fallback to classpath.
 *
 * Direct access to {@link TemplateEngine} is available via require call:
 *
 * <pre>{@code
 * {
 *
 *   TemplateEngine engine = require(TemplateEngine.class);
 *
 * }
 * }</pre>
 *
 * Complete documentation is available at: https://jooby.io/modules/thymeleaf.
 *
 * @author edgar
 * @since 2.0.0
 */
public class ThymeleafModule implements Extension {

  /**
   * Utility class for creating {@link TemplateEngine} instances.
   */
  public static class Builder {

    private ITemplateResolver templateResolver;

    private String templatesPathString = io.jooby.TemplateEngine.PATH;

    private Path templatesPath;

    private Boolean cacheable;

    private ICacheManager cacheManager;

    /**
     * Template path and resolve the path as file system location or fallback to classpath location.
     *
     * @param templatesPath Set template path.
     * @return This builder.
     */
    public @Nonnull Builder setTemplatesPath(@Nonnull String templatesPath) {
      this.templatesPathString = templatesPath;
      return this;
    }

    /**
     * Template path and configure a file system template resolver.
     *
     * @param templatesPath Set template path.
     * @return This builder.
     */
    public @Nonnull Builder setTemplatesPath(@Nonnull Path templatesPath) {
      this.templatesPath = templatesPath;
      return this;
    }

    /**
     * Template resolver to use.
     *
     * @param templateResolver Template resolver to use.
     * @return This builder.
     */
    public @Nonnull Builder setTemplateResolver(@Nonnull ITemplateResolver templateResolver) {
      this.templateResolver = templateResolver;
      return this;
    }

    /**
     * Turn on/off cache. By default cache is enabled when running in <code>dev</code> or
     * <code>test</code> mode.
     *
     * @param cacheable Turn on/off cache.
     * @return This builder.
     */
    public @Nonnull Builder setCacheable(boolean cacheable) {
      this.cacheable = cacheable;
      return this;
    }

    /**
     * Set a cache manager.
     *
     * @param cacheManager Cache manager.
     * @return This builder.
     */
    public @Nonnull Builder setCacheManager(@Nonnull ICacheManager cacheManager) {
      this.cacheManager = cacheManager;
      return this;
    }

    /**
     * Creates a template engine and apply sensible defaults.
     *
     * @param env Environment.
     * @return Template engine.
     */
    public @Nonnull TemplateEngine build(@Nonnull Environment env) {
      TemplateEngine engine = new TemplateEngine();

      if (templateResolver == null) {
        String templatesPathString = normalizePath(
            env.getProperty(TEMPLATE_PATH, ofNullable(this.templatesPathString)
                .orElse(io.jooby.TemplateEngine.PATH))
        );
        templateResolver = defaultTemplateLoader(env, templatesPathString, templatesPath,
            cacheable);
      }

      engine.setTemplateResolver(templateResolver);

      ofNullable(cacheManager).ifPresent(engine::setCacheManager);

      return engine;
    }

    private ITemplateResolver defaultTemplateLoader(Environment env, String templatesPathString,
        Path templatesPath, Boolean cacheable) {
      Path templateDir = ofNullable(templatesPath)
          .orElse(Paths.get(System.getProperty("user.dir"), templatesPathString));
      AbstractConfigurableTemplateResolver resolver;
      if (Files.exists(templateDir)) {
        resolver = new FileTemplateResolver();
        resolver.setPrefix(templateDir.toAbsolutePath().toString());
      } else {
        resolver = new ClassLoaderTemplateResolver(
            env.getClassLoader());
        if (templatesPathString.startsWith("/")) {
          resolver.setPrefix(templatesPathString);
        } else {
          resolver.setPrefix("/" + templatesPathString);
        }
      }
      resolver.setForceSuffix(false);
      resolver.setTemplateMode(TemplateMode.HTML);

      if (cacheable == null) {
        resolver.setCacheable(!env.isActive("dev", "test"));
      } else {
        resolver.setCacheable(cacheable.booleanValue());
      }
      return resolver;
    }
  }

  private static final List<String> EXT = asList(".thl", ".thl.html", ".html");

  private TemplateEngine templateEngine;

  private String templatesPathString = io.jooby.TemplateEngine.PATH;

  private Path templatesPath;

  /**
   * Creates a new module uses the given template engine. You can create a default engine using
   * the {@link #create()} method.
   *
   * @param templateEngine Template engine.
   */
  public ThymeleafModule(@Nonnull TemplateEngine templateEngine) {
    this.templateEngine = templateEngine;
  }

  /**
   * Creates module which look at the given path. It first look at the file-system or fallback
   * to classpath.
   *
   * @param templatesPath Template path.
   */
  public ThymeleafModule(@Nonnull String templatesPath) {
    this.templatesPathString = templatesPath;
  }

  /**
   * Creates module which look at the given path.
   *
   * @param templatesPath Template path.
   */
  public ThymeleafModule(@Nonnull Path templatesPath) {
    this.templatesPath = templatesPath;
  }

  /**
   * Creates module module using the default template path: <code>views</code>. It first look at
   * the file-system or fallback to classpath.
   */
  public ThymeleafModule() {
    this(io.jooby.TemplateEngine.PATH);
  }

  @Override public void install(@Nonnull Jooby application) {
    if (templateEngine == null) {
      templateEngine = create()
          .setTemplatesPath(templatesPath)
          .setTemplatesPath(templatesPathString)
          .build(application.getEnvironment());
    }

    application.encoder(new ThymeleafTemplateEngine(templateEngine, EXT));

    ServiceRegistry services = application.getServices();
    services.put(TemplateEngine.class, templateEngine);
  }

  /**
   * Creates a new thymeleaf builder.
   *
   * @return A builder.
   */
  public static @Nonnull ThymeleafModule.Builder create() {
    return new ThymeleafModule.Builder();
  }
}

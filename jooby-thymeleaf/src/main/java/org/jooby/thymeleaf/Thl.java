package org.jooby.thymeleaf;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Renderer;
import org.jooby.View;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

/**
 * <h1>thymeleaf</h1>
 * <p>
 * <a href="http://www.thymeleaf.org">Thymeleaf</a> is a modern server-side Java template engine for
 * both web and standalone environments.
 * </p>
 *
 * <h2>exports</h2>
 * <ul>
 * <li>{@link TemplateEngine}</li>
 * <li>{@link View.Engine}</li>
 * </ul>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *  use(new Thl());
 *
 *  get("/", () -> {
 *    return Results.html("index")
 *        .put("model", new MyModel());
 *  });
 *
 *  // Or Thymeleaf API:
 *  get("/thymeleaf-api", () -> {
 *    TemplateEngine engine = require(TemplateEngine.class);
 *    engine.processs("template", ...);
 *  });
 * }
 * }</pre>
 *
 * <p>
 * Templates are loaded from root of classpath: <code>/</code> and must end with: <code>.html</code>
 * file extension. Example:
 * </p>
 *
 * <p>
 * public/index.html
 * </p>
 * <pre>{@code
 * <!DOCTYPE html>
 * <html xmlns:th="http://www.thymeleaf.org">
 * <body>
 * <p>
 *     Hello <span th:text="${model.name}">World</span>!!!
 * </p>
 * </body>
 * </html>
 * }</pre>
 *
 * <h2>template loader</h2>
 * <p>
 * Templates are loaded from the <code>root</code> of classpath and must end with
 * <code>.html</code>. You can change the default template location and/or extensions:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Thl("templates", ".thl"));
 * }
 * }</pre>
 *
 * <h2>request locals</h2>
 * <p>
 * A template engine has access to request locals (a.k.a attributes). Here is an example:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Thl());
 *
 *   get("*", req -> {
 *     req.set("foo", bar);
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Then from template:
 * </p>
 *
 * <pre>{@code
 *   <span th:text="${who}">World</span>
 * }</pre>
 *
 * <h2>template cache</h2>
 * <p>
 * Cache is OFF when <code>env=dev</code> (useful for template reloading), otherwise is ON.
 * </p>
 *
 * <h2>advanced configuration</h2>
 * <p>
 * Advanced configuration if provided by callback:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Thl().doWith(engine -> {
 *     engine.addDialect(...);
 *   }));
 * }
 * }</pre>
 *
 * @author edgar
 */
public class Thl implements Jooby.Module {

  private final String prefix;

  private final String suffix;

  private BiConsumer<TemplateEngine, Config> callback;

  /**
   * Creates a new thymeleaf module.
   *
   * @param prefix Template prefix.
   * @param suffix Template suffix.
   */
  public Thl(final String prefix, final String suffix) {
    this.prefix = Objects.requireNonNull(prefix, "Prefix required.");
    this.suffix = Objects.requireNonNull(suffix, "Suffix required.");
  }

  /**
   * Creates a new thymeleaf module.
   */
  public Thl() {
    this("/", ".html");
  }

  /**
   * Set a configuration callback.
   *
   * <pre>{@code
   * {
   *   use(new Thl().doWith(engine -> {
   *     ...
   *   }));
   * }
   * }</pre>
   *
   * @param callback Callback.
   * @return This module.
   */
  public Thl doWith(final Consumer<TemplateEngine> callback) {
    requireNonNull(callback, "Callback required.");
    return doWith((e, c) -> callback.accept(e));
  }

  /**
   * Set a configuration callback.
   *
   * <pre>{@code
   * {
   *   use(new Thl().doWith(engine -> {
   *     ...
   *   }));
   * }
   * }</pre>
   *
   * @param callback Callback.
   * @return This module.
   */
  public Thl doWith(final BiConsumer<TemplateEngine, Config> callback) {
    this.callback = requireNonNull(callback, "Callback required.");
    return this;
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) throws Throwable {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    boolean cacheable = !env.name().equals("dev");
    /** Defaults: */
    resolver.setCacheable(cacheable);
    resolver.setPrefix(prefix);
    resolver.setSuffix(suffix);
    resolver.setTemplateMode(TemplateMode.HTML);

    TemplateEngine engine = new TemplateEngine();
    engine.setTemplateResolver(resolver);

    if (callback != null) {
      callback.accept(engine, conf);
    }

    binder.bind(TemplateEngine.class).toInstance(engine);
    binder.bind(ITemplateEngine.class).toInstance(engine);

    Multibinder.newSetBinder(binder, Renderer.class)
        .addBinding()
        .toInstance(new ThlEngine(engine, env));
  }
}

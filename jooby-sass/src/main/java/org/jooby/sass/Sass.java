package org.jooby.sass;

import static java.util.Objects.requireNonNull;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Route;
import org.jooby.handlers.AssetHandler;
import org.jooby.internal.sass.SassHandler;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

/**
 * <h1>sass</h1>
 *
 * <a href="http://sass-lang.com">Sass handler</a> transform a <code>.scss</code> to
 * <code>.css</code>.
 *
 * <p>
 * Usage:
 * </p>
 *
 * <pre>
 * {
 *   use(new Sass("/css/**"));
 * }
 * </pre>
 *
 * css/style.scss:
 *
 * <pre>
 * $font-stack: Helvetica, sans-serif;
 * $primary-color: #333;
 *
 * body {
 * font: 100% $font-stack;
 * color: $primary-color;
 * }
 * </pre>
 *
 * A request like:
 *
 * <pre>
 * GET /css/style.css
 * </pre>
 *
 * or
 *
 * <pre>
 * GET /css/style.scss
 * </pre>
 *
 * Produces:
 *
 * <pre>
 * body {
 * font: 100% Helvetica, sans-serif;
 * color: #333;
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.9.2
 */

public class Sass implements Jooby.Module {

  private String pattern;

  private String location;

  /**
   * Creates a new {@link Sass} module.
   *
   * @param pattern A route pattern. Used it to matches request against less resources.
   * @param location A location pattern. Used to locate less resources.
   */
  public Sass(final String pattern, final String location) {
    this.pattern = requireNonNull(pattern, "Pattern is required.");
    this.location = requireNonNull(location, "Location pattern is required.");
  }

  /**
   * Creates a new {@link Sass} module.
   *
   * @param pattern A route pattern. Used it to matches request against less resources.
   */
  public Sass(final String pattern) {
    this(pattern, "/");
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    AssetHandler handler = new SassHandler(location)
        .cdn(conf.getString("assets.cdn"))
        .etag(conf.getBoolean("assets.etag"));

    Multibinder.newSetBinder(binder, Route.Definition.class)
        .addBinding()
        .toInstance(new Route.Definition("GET", pattern, handler));
    ;
  }

}

/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.whoops;

import com.typesafe.config.Config;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.internal.whoops.Whoops;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Pretty page error handler for application exceptions. Usage:
 *
 * <pre>{@code
 * {
 *
 *   install(new WhoopsModule());
 *
 * }
 * }</pre>
 *
 * The whoops error handler shows pretty error page as long as we are able to:
 *
 * - Request accepts a text/html response or the accept header is missing.
 * - Works for application code only. It does nothing if no source code is available.
 *
 * @author edgar
 * @since 2.8.0
 */
public class WhoopsModule implements Extension {
  private static final String ENABLED = "whoops.enabled";

  private Path basedir;

  /**
   * Creates new whoops module.
   *
   * @param basedir Base dir.
   */
  public WhoopsModule(@Nonnull Path basedir) {
    this.basedir = basedir;
  }

  /**
   * Creates new whoops module using the <code>user.dir</code> as base directory.
   */
  public WhoopsModule() {
    this(Paths.get(System.getProperty("user.dir")));
  }

  @Override public void install(@Nonnull Jooby application) {
    Config config = application.getConfig();

    boolean enabled = config.hasPath(ENABLED)
        ? config.getBoolean(ENABLED)
        : application.getEnvironment().isActive("dev", "test");

    if (enabled) {
      Whoops whoops = new Whoops(basedir, application.getLog());

      application.assets("/whoops/*", getClass().getPackage().getName().replace(".", "/"));

      application.error(whoops);
    } else {
      application.getLog().debug("Whoops is disabled");
    }
  }
}

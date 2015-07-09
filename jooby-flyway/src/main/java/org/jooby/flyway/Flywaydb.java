package org.jooby.flyway;

import static java.util.Objects.requireNonNull;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.jooby.Env;
import org.jooby.Jooby.Module;
import org.jooby.internal.flyway.FlywayManaged;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * <h1>flyway module</h1>
 * <p>
 * Run database migrations on startup and exposes a {@link Flyway} instance.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *   use(new Jdbc());
 *
 *   use(new Flywaydb());
 * }
 * </pre>
 *
 * <p>
 * Previous example will connect to the {@link DataSource} exposed by the <code>Jdbc</code> module
 * and run the <code>migrate</code> command on startup. This is the recommend way of using
 * {@link Flyway}, there is an alternative approach if you have to migrate two or more databases.
 * </p>
 *
 * <p>
 * If for any reason you need to maintain two or more databases:
 * </p>
 *
 * <pre>
 * flyway.db1.url = "..."
 * flyway.db1.locations = "db1/migration"
 * flyway.db2.url = "..."
 * flyway.db2.locations = "db2/migration"
 * </pre>
 *
 * <pre>
 * {
 *   use(new Flywaydb("flyway.db1"));
 *   use(new Flywaydb("flyway.db2"));
 * }
 * </pre>
 *
 * <h2>migration scripts</h2>
 * <p>
 * {@link Flyway} looks for migration scripts at the <code>db/migration</code> classpath location.
 * We recommend to use <a href="http://semver.org">Semantic versioning</a> for naming the migration
 * scripts:
 * </p>
 *
 * <pre>
 * v0.1.0_My_description.sql
 * v0.1.1_My_small_change.sql
 * </pre>
 *
 * <h2>commands</h2>
 * <p>
 * It is possible to run {@link Flyway} commands on startup, default command is:
 * <code>migrate</code>.
 * </p>
 * <p>
 * If you need to run multiple commands, set the <code>flyway.run</code> property:
 * </p>
 *
 * <pre>
 * flyway.run = [clean, migrate, validate, info]
 * </pre>
 *
 * <h2>configuration</h2>
 * <p>
 * Configuration is done via <code>application.conf</code> under the <code>flyway.*</code> path.
 * There are some defaults setting that you can see in the appendix.
 * </p>
 *
 * @author edgar
 * @since 0.8.0
 */
public class Flywaydb implements Module {

  private String name;

  /**
   * Creates a new {@link Flywaydb}. The given name is use it to read flyway properties, keep in
   * mind that custom configuration will inherited all the properties from <code>flyway.*</code>.
   *
   * @param name Name of the property with flyway properties.
   */
  public Flywaydb(final String name) {
    this.name = requireNonNull(name, "Flyway name is required.");
  }

  /**
   * Creates a new {@link Flywaydb} module, using <code>flyway</code> as property.
   */
  public Flywaydb() {
    this("flyway");
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    Config $flyway = config.getConfig(name)
        .withFallback(config.getConfig("flyway"));

    if ($flyway.hasPath("url")) {
      // standalone flyway (no datasource dep)
      binder.bind(Flyway.class).toProvider(new FlywayManaged($flyway)).asEagerSingleton();
    } else {
      binder.bind(Key.get(Config.class, Names.named("flyway.conf"))).toInstance($flyway);
      binder.bind(Flyway.class).toProvider(FlywayManaged.class).asEagerSingleton();
    }
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "flyway.conf");
  }
}

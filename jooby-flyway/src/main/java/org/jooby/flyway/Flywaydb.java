/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.flyway;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.flywaydb.core.Flyway;
import org.jooby.Env;
import org.jooby.Jooby.Module;

import com.google.inject.Binder;
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
 * flyway.url = ...
 * flyway.user = ...
 * flyway.password = ...
 * </pre>
 *
 * <pre>
 * {
 *   use(new Flywaydb());
 * }
 * </pre>
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

  private static enum Command {
    migrate {
      @Override
      public void run(final Flyway flyway) {
        flyway.migrate();
      }
    },

    clean {
      @Override
      public void run(final Flyway flyway) {
        flyway.clean();
      }
    },

    info {
      @Override
      public void run(final Flyway flyway) {
        flyway.info();
      }
    },

    validate {
      @Override
      public void run(final Flyway flyway) {
        flyway.validate();
      }
    },

    baseline {
      @Override
      public void run(final Flyway flyway) {
        flyway.baseline();
      }
    },

    repair {
      @Override
      public void run(final Flyway flyway) {
        flyway.repair();
      }
    };

    public abstract void run(Flyway flyway);
  }

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

    Flyway flyway = new Flyway();
    flyway.configure(props($flyway));
    commands($flyway).forEach(cmd -> cmd.run(flyway));
    binder.bind(Flyway.class).toInstance(flyway);
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private Properties props(final Config config) {
    Properties props = new Properties();
    config.withoutPath("run").entrySet().forEach(prop -> {
      Object value = prop.getValue().unwrapped();
      if (value instanceof List) {
        value = ((List) value).stream().collect(Collectors.joining(","));
      }
      props.setProperty("flyway." + prop.getKey(), value.toString());
    });
    return props;
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "flyway.conf");
  }

  @SuppressWarnings("unchecked")
  private Iterable<Command> commands(final Config config) {
    Object value = config.getAnyRef("run");
    List<String> commands = new ArrayList<>();
    if (value instanceof List) {
      commands.addAll((List<? extends String>) value);
    } else {
      commands.add(value.toString());
    }
    return commands.stream()
        .map(command -> Command.valueOf(command.toLowerCase()))
        .collect(Collectors.toList());
  }
}

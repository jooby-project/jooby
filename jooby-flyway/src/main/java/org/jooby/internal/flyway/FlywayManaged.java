package org.jooby.internal.flyway;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.jooby.Managed;

import com.google.inject.name.Named;
import com.typesafe.config.Config;

public class FlywayManaged implements Provider<Flyway>, Managed {

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

  private Flyway flyway;

  private Iterable<Command> commands;

  @Inject
  public FlywayManaged(final DataSource ds, @Named("flyway.conf") final Config config) {
    flyway = new Flyway();
    flyway.configure(props(config));
    flyway.setDataSource(ds);
    commands = commands(config);
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

  public FlywayManaged(final Config config) {
    flyway = new Flyway();
    flyway.configure(props(config));
    commands = commands(config);
  }

  @Override
  public void start() throws Exception {
    commands.forEach(cmd -> cmd.run(flyway));
  }

  @Override
  public void stop() throws Exception {
  }

  @Override
  public Flyway get() {
    return flyway;
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

}

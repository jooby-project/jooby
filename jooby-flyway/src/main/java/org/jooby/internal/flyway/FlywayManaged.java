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

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
package org.jooby.jdbc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.Jooby;
import org.jooby.Mode;
import org.jooby.fn.Switch;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Jdbc extends Jooby.Module {

  public static final String DEFAULT_DB = "db";

  private abstract static class DataSourceHolder implements Provider<DataSource> {
    private DataSource dataSource;

    private String name;

    public DataSourceHolder(final String name) {
      this.name = name;
    }

    public DataSource getOrCreate() throws Exception {
      if (dataSource == null) {
        dataSource = doGet();
      }
      return dataSource;
    }

    @Override
    public DataSource get() {
      try {
        return getOrCreate();
      } catch (RuntimeException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new IllegalStateException("Unable to get " + DataSource.class.getName(), ex);
      }
    }

    protected abstract DataSource doGet() throws Exception;

    public void shutdown() throws Exception {
      if (dataSource instanceof HikariDataSource) {
        ((HikariDataSource) dataSource).shutdown();
        dataSource = null;
      }
    }

    @Override
    public final String toString() {
      return name;
    }
  }

  private final String dbName;

  private DataSourceHolder ds;

  public Jdbc(final String name) {
    checkArgument(name != null && name.length() > 0, "A database name is required.");
    this.dbName = DEFAULT_DB.equals(name) ? name : DEFAULT_DB + "." + name;
  }

  public Jdbc() {
    this(DEFAULT_DB);
  }

  @Override
  public void configure(final Mode mode, final Config config, final Binder binder)
      throws Exception {
    this.ds = newDataSource(mode, dbName, dbConfig(dbName, config));

    binder.bind(dataSourceKey(DataSource.class))
        .toProvider(ds)
        .in(Scopes.SINGLETON);
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(Jdbc.class, "jdbc.conf");
  }

  @Override
  public void stop() throws Exception {
    if (ds != null) {
      ds.shutdown();
      ds = null;
    }
  }

  private Config dbConfig(final String key, final Config source) throws Exception {
    String db = source.getAnyRef(key).toString();

    Function<String, Config> h2 = (url) -> ConfigFactory.empty()
        .withValue("db.url", ConfigValueFactory.fromAnyRef(url))
        .withValue("db.type", ConfigValueFactory.fromAnyRef("h2"))
        .withValue("db.user", ConfigValueFactory.fromAnyRef("sa"))
        .withValue("db.password", ConfigValueFactory.fromAnyRef(""))
        .withFallback(source);

    return Switch.<Config> newSwitch(db)
        .when("mem", () -> {
          String url = "jdbc:h2:mem:db;DB_CLOSE_DELAY=-1";
          return h2.apply(url);
        })
        .when("fs",
            () -> {
              final String dbName = DEFAULT_DB.equals(key) ? source.getString("application.name")
                  : key;
              String url = "jdbc:h2:"
                  + new File(source.getString("jooby.io.tmpdir"), dbName).getAbsolutePath();
              return h2.apply(url);
            })
        .value().orElse(source);
  }

  private DataSourceHolder newDataSource(final Mode mode, final String key, final Config config)
      throws Exception {
    Properties props = new Properties();

    Config $hikari = config.getConfig("hikari").withoutPath("profiles");

    BiConsumer<String, Entry<String, ConfigValue>> dumper = (prefix, entry) -> {
      String propertyName = prefix + entry.getKey();
      String[] path = propertyName.split("\\.");

      if (path.length <= 2) {
        String propertyValue = entry.getValue().unwrapped().toString();
        props.setProperty(propertyName, propertyValue);
      }
    };

    /**
     * Dump hikari properties.
     */
    $hikari.entrySet().forEach(entry -> dumper.accept("", entry));

    /**
     * Dump dataSource.* properties
     */
    Config $db = config.getConfig(key);
    $db.withoutPath("type").entrySet().forEach(entry -> dumper.accept("dataSource.", entry));
    // append profile!
    if ($db.hasPath("type")) {
      config.getConfig("hikari.profiles." + $db.getString("type"))
          .entrySet().forEach(entry -> dumper.accept("dataSource.", entry));
    }

    mode.ifMode("dev", () -> props.setProperty("maximumPoolSize", "1"));

    if (!props.containsKey("dataSourceClassName")) {
      // adjust dataSourceClassName when missing
      props.setProperty("dataSourceClassName", props.getProperty("dataSource.dataSourceClassName"));
    }
    // remove dataSourceClassName under dataSource
    props.remove("dataSource.dataSourceClassName");

    return new DataSourceHolder(DEFAULT_DB.equals(key) ? config.getString("application.name") : key) {
      @Override
      protected DataSource doGet() throws Exception {
        return new HikariDataSource(new HikariConfig(props));
      }
    };
  }

  protected final <T> Key<T> dataSourceKey(final Class<T> type) {
    return DEFAULT_DB.equals(dbName) ? Key.get(type) : Key.get(type, Names.named(dbName));
  }

  protected final Provider<DataSource> dataSource() {
    checkState(ds != null, "Data source isn't reqdy yet");
    return ds;
  }
}

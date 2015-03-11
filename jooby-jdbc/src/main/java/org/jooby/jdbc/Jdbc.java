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

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.function.BiConsumer;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.Env;
import org.jooby.Jooby;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import com.typesafe.config.ConfigValueFactory;
import com.zaxxer.hikari.HikariConfig;

public class Jdbc implements Jooby.Module {

  public static final String DEFAULT_DB = "db";

  private final String dbName;

  private HikariDataSourceProvider ds;

  private Optional<String> dbtype;

  public Jdbc(final String name) {
    checkArgument(name != null && name.length() > 0, "A database name is required.");
    this.dbName = DEFAULT_DB.equals(name) ? name : DEFAULT_DB + "." + name;
  }

  public Jdbc() {
    this(DEFAULT_DB);
  }

  @Override
  public void configure(final Env mode, final Config config, final Binder binder) {
    this.ds = newDataSource(dbName, dbConfig(dbName, config));

    binder.bind(dataSourceKey(DataSource.class))
        .toProvider(ds).asEagerSingleton();
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(Jdbc.class, "jdbc.conf");
  }

  private Config dbConfig(final String key, final Config source) {
    Object db = source.getAnyRef(key);

    if (db instanceof String) {
      String embeddeddb = "databases." + db;
      if (db.toString().indexOf(':') == -1 && source.hasPath(embeddeddb)) {
        Config dbtree = source.getConfig(embeddeddb);
        dbtree = dbtree.withValue("url", ConfigValueFactory.fromAnyRef(
                  dbtree.getString("url").replace("{mem.seed}", System.currentTimeMillis() + "")
               ));
        // write embedded with current key
        return ConfigFactory.empty()
            .withValue(key, dbtree.root())
            .withFallback(source);
      } else {
        // assume it is a just the url
        return ConfigFactory.empty()
            .withValue(key + ".url", ConfigValueFactory.fromAnyRef(db.toString()))
            .withFallback(source);
      }
    } else {
      return source;
    }
  }

  private HikariDataSourceProvider newDataSource(final String key, final Config config) {
    Properties props = new Properties();

    BiConsumer<String, Entry<String, ConfigValue>> dumper = (prefix, entry) -> {
      String propertyName = prefix + entry.getKey();
      String[] path = propertyName.split("\\.");

      if (path.length <= 2) {
        String propertyValue = entry.getValue().unwrapped().toString();
        props.setProperty(propertyName, propertyValue);
      }
    };

    String hikaryKey = "hikari" + (DEFAULT_DB.equals(key) ? "" : key.replace(DEFAULT_DB, ""));
    Config $hikari = config.hasPath(hikaryKey) ? config.getConfig(hikaryKey) : ConfigFactory
        .empty();

    // figure it out db type.
    dbtype = dbtype(key, config);

    /**
     * dump properties from less to higher precedence
     *
     * # databases.[type]
     * # db.* -> dataSource.*
     * # hikari.* -> * (no prefix)
     */
    dbtype.ifPresent(type ->
        config.getConfig("databases." + type)
            .entrySet().forEach(entry -> dumper.accept("dataSource.", entry))
        );

    config.getConfig(key).entrySet().forEach(entry -> dumper.accept("dataSource.", entry));

    $hikari.entrySet().forEach(entry -> dumper.accept("", entry));

    if (!props.containsKey("dataSourceClassName")) {
      // adjust dataSourceClassName when missing
      props.setProperty("dataSourceClassName", props.getProperty("dataSource.dataSourceClassName"));
    }
    // remove dataSourceClassName under dataSource
    props.remove("dataSource.dataSourceClassName");
    // set pool name
    props.setProperty("poolName", dbtype.map(type -> type + "." + dbName).orElse(dbName));

    return new HikariDataSourceProvider(new HikariConfig(props));
  }

  private Optional<String> dbtype(final String key, final Config config) {
    String url = config.getString(key + ".url");
    String type = Arrays.stream(url.toLowerCase().split(":"))
        .filter(token -> !(token.equals("jdbc") || token.equals("jtds")))
        .findFirst()
        .get();

    if (config.hasPath("databases." + type)) {
      return Optional.of(type);
    }
    return Optional.empty();
  }

  protected final <T> Key<T> dataSourceKey(final Class<T> type) {
    return DEFAULT_DB.equals(dbName) ? Key.get(type) : Key.get(type, Names.named(dbName));
  }

  protected final Provider<DataSource> dataSource() {
    checkState(ds != null, "Data source isn't ready yet");
    return ds;
  }
}

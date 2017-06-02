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
package org.jooby.cassandra;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.raw;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.ttl;
import static java.util.Objects.requireNonNull;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.Session;
import org.jooby.Session.Builder;
import org.jooby.Session.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.schemabuilder.Create;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import javaslang.Lazy;

/**
 * <h1>cassandra session store</h1>
 * <p>
 * A {@link Session.Store} powered by <a href="http://cassandra.apache.org">Cassandra</a>.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *   use(new Cassandra("cassandra://localhost/db"));
 *
 *   session(CassandraSessionStore.class);
 *
 *   get("/", req -> {
 *     Session session = req.session();
 *     session.put("foo", "bar");
 *
 *     ..
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Session data is persisted in Cassandra using a <code>session</code> table.
 * </p>
 *
 * <h2>options</h2>
 *
 * <h3>timeout</h3>
 * <p>
 * By default, a session will expire after <code>30 minutes</code>. Changing the default timeout is
 * as simple as:
 * </p>
 *
 * <pre>
 * # 8 hours
 * session.timeout = 8h
 *
 * # 15 seconds
 * session.timeout = 15
 *
 * # 120 minutes
 * session.timeout = 120m
 * </pre>
 *
 * <p>
 * Expiration is done via Cassandra ttl option.
 * </p>
 *
 * <p>
 * If no timeout is required, use <code>-1</code>.
 * </p>
 *
 * @author edgar
 * @since 1.0.0.CR7
 */
public class CassandraSessionStore implements Store {

  private static final String TIMEOUT = "timeout";

  private static final String ID = "id";

  private static final String ATTRIBUTES = "attributes";

  private static final String SAVED_AT = "savedAt";

  private static final String ACCESSED_AT = "accessedAt";

  private static final String CREATED_AT = "createdAt";

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final com.datastax.driver.core.Session session;

  private final int timeout;

  private final String tableName = "session";

  private final Lazy<PreparedStatement> insertSQL;

  private Lazy<PreparedStatement> selectSQL;

  private Lazy<PreparedStatement> deleteSQL;

  public CassandraSessionStore(final com.datastax.driver.core.Session session, final int timeout) {
    this.session = requireNonNull(session, "Session required.");
    this.timeout = timeout;

    createTableIfNotExists(session, tableName, log);

    this.insertSQL = Lazy.of(() -> session.prepare(insertSQL(tableName, timeout)));
    this.selectSQL = Lazy.of(() -> session.prepare(selectSQL(tableName)));
    this.deleteSQL = Lazy.of(() -> session.prepare(deleteSQL(tableName)));
  }

  @Inject
  public CassandraSessionStore(final com.datastax.driver.core.Session session,
      final @Named("session.timeout") String timeout) {
    this(session, seconds(timeout));
  }

  @Override
  public Session get(final Builder builder) {
    ResultSet rs = session
        .execute(new BoundStatement(selectSQL.get()).bind(builder.sessionId()));
    return Optional.ofNullable(rs.one())
        .map(row -> {
          long createdAt = row.getTimestamp(CREATED_AT).getTime();
          long accessedAt = row.getTimestamp(ACCESSED_AT).getTime();
          long savedAt = row.getTimestamp(SAVED_AT).getTime();
          Map<String, String> attributes = row.getMap(ATTRIBUTES, String.class, String.class);
          Session session = builder
              .accessedAt(accessedAt)
              .createdAt(createdAt)
              .savedAt(savedAt)
              .set(attributes)
              .build();
          // touch ttl
          if (timeout > 0) {
            save(session);
          }
          return session;
        })
        .orElse(null);
  }

  @Override
  public void save(final Session session) {
    this.session.execute(new BoundStatement(insertSQL.get())
        .bind(
            session.id(),
            new Date(session.createdAt()),
            new Date(session.accessedAt()),
            new Date(session.savedAt()),
            session.attributes()));
  }

  @Override
  public void create(final Session session) {
    save(session);
  }

  @Override
  public void delete(final String id) {
    session.execute(new BoundStatement(deleteSQL.get()).bind(id));
  }

  private static int seconds(final String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      Config config = ConfigFactory.empty()
          .withValue(TIMEOUT, ConfigValueFactory.fromAnyRef(value));
      return (int) config.getDuration(TIMEOUT, TimeUnit.SECONDS);
    }
  }

  private static void createTableIfNotExists(final com.datastax.driver.core.Session session,
      final String table, final Logger log) {
    Create createTable = SchemaBuilder.createTable(table)
        .addPartitionKey(ID, DataType.varchar())
        .addColumn(CREATED_AT, DataType.timestamp())
        .addColumn(ACCESSED_AT, DataType.timestamp())
        .addColumn(SAVED_AT, DataType.timestamp())
        .addColumn(ATTRIBUTES, DataType.map(DataType.varchar(), DataType.varchar()))
        .ifNotExists();

    Futures.addCallback(session.executeAsync(createTable), new FutureCallback<ResultSet>() {
      @Override
      public void onSuccess(final ResultSet result) {
        log.debug("Session table successfully created");
      }

      @Override
      public void onFailure(final Throwable x) {
        log.error("Create session table resulted in exception", x);
      }
    });
  }

  private static String selectSQL(final String table) {
    return select().from(table).where(eq(ID, raw("?"))).getQueryString();
  }

  private static String deleteSQL(final String table) {
    return QueryBuilder.delete().from(table).where(eq(ID, raw("?"))).getQueryString();
  }

  private static String insertSQL(final String table, final int timeout) {
    Insert insertInto = insertInto(table)
        .value(ID, raw("?"))
        .value(CREATED_AT, raw("?"))
        .value(ACCESSED_AT, raw("?"))
        .value(SAVED_AT, raw("?"))
        .value(ATTRIBUTES, raw("?"));
    if (timeout > 0) {
      insertInto.using(ttl(timeout));
    }
    return insertInto.getQueryString();
  }

}

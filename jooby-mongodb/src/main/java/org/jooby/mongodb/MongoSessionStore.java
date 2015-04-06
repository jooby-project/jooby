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
package org.jooby.mongodb;

import static java.util.Objects.requireNonNull;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.Session;
import org.jooby.Session.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/**
 * A {@link Session.Store} powered by
 * <a href="http://mongodb.github.io/mongo-java-driver/">Mongodb</a>.
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *   use(new Mongodb());
 *
 *   session(MongoSessionStore.class);
 *
 *   get("/", req {@literal ->} {
 *    req.session().set("name", "jooby");
 *   });
 * }
 * </pre>
 *
 * The <code>name</code> attribute and value will be stored in a
 * <a href="http://mongodb.github.io/mongo-java-driver/">Mongodb</a>.
 *
 * <h2>options</h2>
 *
 * <h3>timeout</h3>
 * <p>
 * By default, a mongodb session will expire after <code>30 minutes</code>. Changing the default
 * timeout is as simple as:
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
 * It uses MongoDB's TTL collection feature (2.2+) to have <code>mongod</code> automatically remove
 * expired sessions.
 * </p>
 *
 * If no timeout is required, use <code>-1</code>.
 *
 * <h3>session collection</h3>
 * <p>
 * Default mongodb collection is <code>sessions</code>.
 *
 * <p>
 * It's possible to change the default key setting the <code>mongodb.sesssion.collection</code>
 * properties.
 * </p>
 *
 * @author edgar
 * @since 0.5.0
 */
public class MongoSessionStore implements Session.Store {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  protected final DBCollection sessions;

  protected final int timeout;

  protected final String collection;

  private final AtomicBoolean ttlSync = new AtomicBoolean(false);

  protected final DB db;

  public MongoSessionStore(final DB db, final String collection, final int timeout) {
    this.db = requireNonNull(db, "Mongo db is required.");
    this.collection = requireNonNull(collection, "Collection is required.");
    this.sessions = db.getCollection(collection);
    this.timeout = timeout;
  }

  @Inject
  public MongoSessionStore(final DB db,
      final @Named("mongodb.session.collection") String collection,
      final @Named("session.timeout") String timeout) {
    this(db, collection, seconds(timeout));
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  public Session get(final Builder builder) {
    return Optional.ofNullable(sessions.findOne(builder.sessionId())).map(dbobj -> {
      Map session = dbobj.toMap();

      Date accessedAt = (Date) session.remove("_accessedAt");
      Date createdAt = (Date) session.remove("_createdAt");
      Date savedAt = (Date) session.remove("_savedAt");
      session.remove("_id");

      return builder
          .accessedAt(accessedAt.getTime())
          .createdAt(createdAt.getTime())
          .savedAt(savedAt.getTime())
          .set(session)
          .build();
    }).orElse(null);
  }

  @Override
  public void save(final Session session) {
    syncTtl();

    BasicDBObjectBuilder ob = BasicDBObjectBuilder.start()
        .add("_id", session.id())
        .add("_accessedAt", new Date(session.accessedAt()))
        .add("_createdAt", new Date(session.createdAt()))
        .add("_savedAt", new Date(session.savedAt()));
    // dump attributes
    session.attributes().forEach((k, v) -> ob.add(k, v));

    sessions.save(ob.get());
  }

  @Override
  public void create(final Session session) {
    save(session);
  }

  private void syncTtl() {
    if (!ttlSync.get()) {
      ttlSync.set(true);

      if (timeout <= 0) {
        return;
      }

      try {
        log.debug("creating session timeout index");
        sessions.createIndex(
            new BasicDBObject("_accessedAt", 1),
            new BasicDBObject("expireAfterSeconds", timeout)
            );
      } catch (MongoException ex) {
        log.debug("Couldn't update session timeout, we are going to update session timeout", ex);
        // TODO: allow to customize? ... not sure
        db.command(BasicDBObjectBuilder.start()
            .add("collMod", collection)
            .add("index", BasicDBObjectBuilder.start()
                .add("keyPattern", new BasicDBObject("_accessedAt", 1))
                .add("expireAfterSeconds", timeout)
                .get()
            )
            .get()
            );
      }
    }
  }

  @Override
  public void delete(final String id) {
    sessions.remove(new BasicDBObject("_id", id));
  }

  private static int seconds(final String value) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      Config config = ConfigFactory.empty()
          .withValue("timeout", ConfigValueFactory.fromAnyRef(value));
      return (int) config.getDuration("timeout", TimeUnit.SECONDS);
    }
  }

}

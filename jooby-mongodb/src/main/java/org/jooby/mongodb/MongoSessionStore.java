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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.jooby.Session;
import org.jooby.Session.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
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

  private static final String SESSION_IDX = "_sessionIdx_";

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  protected final MongoCollection<Document> sessions;

  protected final long timeout;

  protected final String collection;

  private final AtomicBoolean ttlSync = new AtomicBoolean(false);

  protected final MongoDatabase db;

  public MongoSessionStore(final MongoDatabase db, final String collection,
      final long timeoutInSeconds) {
    this.db = requireNonNull(db, "Mongo db is required.");
    this.collection = requireNonNull(collection, "Collection is required.");
    this.sessions = db.getCollection(collection);
    this.timeout = timeoutInSeconds;
  }

  @Inject
  public MongoSessionStore(final MongoDatabase db,
      final @Named("mongodb.session.collection") String collection,
      final @Named("session.timeout") String timeout) {
    this(db, collection, seconds(timeout));
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  public Session get(final Builder builder) {
    return Optional.ofNullable(sessions.find(Filters.eq("_id", builder.sessionId())).first())
        .map(doc -> {
          Map session = new LinkedHashMap<>(doc);

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

    String id = session.id();
    Bson filter = Filters.eq("_id", id);

    Document doc = new Document()
        .append("_id", id)
        .append("_accessedAt", new Date(session.accessedAt()))
        .append("_createdAt", new Date(session.createdAt()))
        .append("_savedAt", new Date(session.savedAt()));
    // dump attributes
    session.attributes().forEach((k, v) -> doc.append(k, v));

    sessions.updateOne(filter, new Document("$set", doc), new UpdateOptions().upsert(true));
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

      log.debug("creating session timeout index");
      if (existsIdx(SESSION_IDX)) {
        Document command = new Document("collMod", collection)
            .append("index",
                new Document("keyPattern", new Document("_accessedAt", 1))
                    .append("expireAfterSeconds", timeout));
        log.debug("{}", command);
        Document result = db.runCommand(command);
        log.debug("{}", result);
      } else {
        sessions.createIndex(
            new Document("_accessedAt", 1),
            new IndexOptions()
                .name(SESSION_IDX)
                .expireAfter(timeout, TimeUnit.SECONDS));
      }
    }
  }

  @Override
  public void delete(final String id) {
    sessions.deleteOne(new Document("_id", id));
  }

  private static long seconds(final String value) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      Config config = ConfigFactory.empty()
          .withValue("timeout", ConfigValueFactory.fromAnyRef(value));
      return config.getDuration("timeout", TimeUnit.SECONDS);
    }
  }

  private boolean existsIdx(final String name) {
    MongoCursor<Document> iterator = sessions.listIndexes().iterator();
    while (iterator.hasNext()) {
      Document doc = iterator.next();
      if (doc.getString("name").equals(name)) {
        return true;
      }
    }
    return false;
  }

}

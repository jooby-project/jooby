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
package org.jooby.couchbase;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.Session;
import org.jooby.Session.Builder;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/**
 * <h1>couchbase session store</h1>
 * <p>
 * A {@link Session.Store} powered by <a href="http://www.couchbase.com">Couchbase</a>.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *   use(new Couchbase("couchbase://localhost/bucket"));
 *
 *   session(CouchbaseSessionStore.class);
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
 * Session data is persisted in Couchbase and document looks like:
 * </p>
 *
 * <pre>{@code
 * {
 *   "session::{SESSION_ID}": {
 *     "foo": "bar"
 *   }
 * }
 * }</pre>
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
 * Expiration is done via Couchbase expiry/ttl option.
 * </p>
 *
 * <p>
 * If no timeout is required, use <code>-1</code>.
 * </p>
 *
 * <h3>custom bucket</h3>
 * <p>
 * The session document are persisted in the application/default bucket, if you need/want a
 * different bucket then use {@link Couchbase#sessionBucket(String)}, like:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(
 *       new Couchbase("couchbase://localhost/myapp")
 *           .sessionBucket("session")
 *   );
 * }
 * }</pre>
 *
 * @author edgar
 * @since 1.0.0.CR7
 */
@SuppressWarnings({"unchecked", "rawtypes" })
public class CouchbaseSessionStore implements Session.Store {

  private static final String SESSION = "session";

  private Bucket bucket;

  private int expiry;

  public CouchbaseSessionStore(final Bucket bucket, final int timeout) {
    this.bucket = requireNonNull(bucket, "Bucket is required.");
    this.expiry = timeout > 0 ? timeout : 0;
  }

  @Inject
  public CouchbaseSessionStore(final @Named(SESSION) Bucket bucket,
      final @Named("session.timeout") String timeout) {
    this(bucket, seconds(timeout));
  }

  @Override
  public Session get(final Builder builder) {
    return Optional
        .ofNullable(bucket.getAndTouch(N1Q.qualifyId(SESSION, builder.sessionId()), expiry))
        .map(doc -> {
          Map session = doc.content().toMap();

          Long accessedAt = (Long) session.remove("_accessedAt");
          Long createdAt = (Long) session.remove("_createdAt");
          Long savedAt = (Long) session.remove("_savedAt");

          return builder
              .accessedAt(accessedAt)
              .createdAt(createdAt)
              .savedAt(savedAt)
              .set(session)
              .build();
        }).orElse(null);
  }

  @Override
  public void save(final Session session) {
    JsonObject json = JsonObject.from(session.attributes());

    // session metadata
    json.put("_accessedAt", session.accessedAt());
    json.put("_createdAt", session.createdAt());
    json.put("_savedAt", session.savedAt());

    JsonDocument doc = JsonDocument.create(N1Q.qualifyId(SESSION, session.id()), expiry, json);
    bucket.upsert(doc);
  }

  @Override
  public void create(final Session session) {
    save(session);
  }

  @Override
  public void delete(final String id) {
    bucket.remove(N1Q.qualifyId(SESSION, id));
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

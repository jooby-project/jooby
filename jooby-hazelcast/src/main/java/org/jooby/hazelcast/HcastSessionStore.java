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
package org.jooby.hazelcast;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.jooby.Session;
import org.jooby.Session.Builder;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/**
 * A {@link Session.Store} powered by <a href="http://hazelcast.org">Hazelcast</a>.
 *
 * <pre>
 * {
 *   use(new Hcast());
 *
 *   session(HcastSessionStore.class);
 *
 *   get("/", req {@literal ->} {
 *    req.session().set("name", "jooby");
 *   });
 * }
 * </pre>
 *
 * <h2>options</h2>
 *
 * <h3>timeout</h3>
 * <p>
 * By default, a hazelcast session will expire after <code>30 minutes</code>. Changing the default
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
 *
 * # no timeout
 * session.timeout = -1
 * </pre>
 *
 * <h3>name</h3>
 * <p>
 * Default session's name is <code>sessions</code>. It's possible to change the default name by
 * setting the property: <code>hazelcast.sesssion.name</code>.
 * </p>
 *
 * @author edgar
 * @since 0.9.0
 */
public class HcastSessionStore implements Session.Store {

  private IMap<String, Map<String, String>> sessions;

  private int timeout;

  /**
   * Creates a new {@link HcastSessionStore}.
   *
   * @param hazelcast Hazelcast instance.
   * @param name Session name.
   * @param timeout Session timeout expression, like <code>30m</code>.
   */
  @Inject
  public HcastSessionStore(final HazelcastInstance hazelcast,
      @Named("hazelcast.session.name") final String name,
      @Named("hazelcast.session.timeout") final String timeout) {
    this(hazelcast, name, seconds(timeout));
  }

  /**
   * Creates a new {@link HcastSessionStore}.
   *
   * @param hazelcast Hazelcast instance.
   * @param name Session name.
   * @param timeout Session timeout in seconds.
   */
  public HcastSessionStore(final HazelcastInstance hazelcast, final String name,
      final int timeout) {
    requireNonNull(hazelcast, "Hazelcast is required.");
    this.sessions = hazelcast.getMap(name);
    this.timeout = timeout > 0 ? timeout : 0;
  }

  @Override
  public Session get(final Builder builder) {
    Map<String, String> attrs = sessions.get(builder.sessionId());
    if (attrs == null) {
      return null;
    }
    return builder
        .accessedAt(Long.parseLong(attrs.remove("_accessedAt")))
        .createdAt(Long.parseLong(attrs.remove("_createdAt")))
        .savedAt(Long.parseLong(attrs.remove("_savedAt")))
        .set(attrs)
        .build();
  }

  @Override
  public void save(final Session session) {
    Map<String, String> attrs = new HashMap<>(session.attributes());
    attrs.put("_createdAt", Long.toString(session.createdAt()));
    attrs.put("_accessedAt", Long.toString(session.accessedAt()));
    attrs.put("_savedAt", Long.toString(session.savedAt()));

    sessions.set(session.id(), attrs, timeout, TimeUnit.SECONDS);
  }

  @Override
  public void create(final Session session) {
    save(session);
  }

  @Override
  public void delete(final String id) {
    sessions.remove(id);
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

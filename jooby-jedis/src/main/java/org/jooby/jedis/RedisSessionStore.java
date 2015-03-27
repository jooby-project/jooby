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
package org.jooby.jedis;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jooby.Session;
import org.jooby.Session.Builder;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Singleton
public class RedisSessionStore implements Session.Store {

  private JedisPool pool;

  @Inject
  public RedisSessionStore(final JedisPool pool) {
    this.pool = requireNonNull(pool, "Jedis pool is required.");
  }

  @Override
  public Session get(final Builder builder) {
    try (Jedis jedis = pool.getResource()) {
      Map<String, String> attrs = jedis.hgetAll("sessions:" + builder.sessionId());
      return builder
          .accessedAt(Long.parseLong(attrs.remove("accessedAt")))
          .createdAt(Long.parseLong(attrs.remove("createdAt")))
          .savedAt(Long.parseLong(attrs.remove("savedAt")))
          .set(attrs).build();
    }
  }

  @Override
  public void save(final Session session) {
    try (Jedis jedis = pool.getResource()) {
      String key = "sessions:" + session.id();
      Map<String, String> attrs = new HashMap<>(session.attributes());
      attrs.put("createdAt", Long.toString(session.createdAt()));
      attrs.put("accessedAt", Long.toString(session.accessedAt()));
      attrs.put("savedAt", Long.toString(session.savedAt()));
      jedis.hmset(key, attrs);
    }
  }

  @Override
  public void create(final Session session) {
    save(session);
  }

  @Override
  public void delete(final String id) {
    try (Jedis jedis = pool.getResource()) {
      String key = "sessions:" + id;
      jedis.del(key);
    }

  }

}

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
package org.jooby.memcached;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import net.spy.memcached.MemcachedClient;

import org.jooby.Session;
import org.jooby.Session.Builder;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class SpySessionStore implements Session.Store {

  private MemcachedClient memcached;

  private String prefix;

  private int timeout;

  public SpySessionStore(final MemcachedClient memcached,
      final String prefix, final int timeoutInSeconds) {
    this.memcached = memcached;
    this.prefix = prefix;
    this.timeout = timeoutInSeconds;
  }

  @Inject
  public SpySessionStore(final MemcachedClient memcached,
      final @Named("memcached.session.prefix") String prefix,
      @Named("memcached.session.timeout") final String timeout) {
    this(memcached, prefix, seconds(timeout));
  }

  @SuppressWarnings("unchecked")
  @Override
  public Session get(final Builder builder) {
    String key = key(builder.sessionId());
    Map<String, String> attrs = (Map<String, String>) memcached.get(key);

    if (attrs == null || attrs.size() == 0) {
      // expired
      return null;
    }

    // touch session
    memcached.touch(key, timeout);

    return builder
        .accessedAt(Long.parseLong(attrs.remove("_accessedAt")))
        .createdAt(Long.parseLong(attrs.remove("_createdAt")))
        .savedAt(Long.parseLong(attrs.remove("_savedAt")))
        .set(attrs)
        .build();
  }

  @Override
  public void save(final Session session) {
    String key = key(session);

    Map<String, String> attrs = new HashMap<>(session.attributes());
    attrs.put("_createdAt", Long.toString(session.createdAt()));
    attrs.put("_accessedAt", Long.toString(session.accessedAt()));
    attrs.put("_savedAt", Long.toString(session.savedAt()));

    memcached.set(key, timeout, attrs);
  }

  @Override
  public void create(final Session session) {
    save(session);
  }

  @Override
  public void delete(final String id) {
    String key = key(id);

    memcached.replace(key, 1, new HashMap<>());
  }

  private String key(final String id) {
    return prefix + ":" + id;
  }

  private String key(final Session session) {
    return key(session.id());
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

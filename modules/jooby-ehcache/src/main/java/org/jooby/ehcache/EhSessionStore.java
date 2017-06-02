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
package org.jooby.ehcache;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.jooby.Session;
import org.jooby.Session.Builder;

/**
 * {@link Session.Store} powered by an {@link Ehcache}. In order to use this store a
 * <code>session</code> cache is required.
 *
 * <pre>
 * ehcache.cache.session {
 *   timeToIdle = 30m
 * }
 * </pre>
 *
 * In this example cache will expire after 30 minutes of inactivity. But of course, you can do
 * whatever you want/need.
 *
 * @author edgar
 * @since 0.6.0
 */
public class EhSessionStore implements Session.Store {

  private Ehcache cache;

  @Inject
  public EhSessionStore(@Named("session") final Ehcache cache) {
    this.cache = requireNonNull(cache, "Sessions cache is required.");
  }

  @SuppressWarnings("unchecked")
  @Override
  public Session get(final Builder builder) {
    String sessionId = builder.sessionId();
    Element element = cache.get(sessionId);
    if (element == null) {
      return null;
    }
    Map<String, String> attrs = new HashMap<>((Map<String, String>) element.getObjectValue());
    return builder
        .accessedAt(Long.parseLong(attrs.remove("_accessedAt")))
        .createdAt(Long.parseLong(attrs.remove("_createdAt")))
        .savedAt(Long.parseLong(attrs.remove("_savedAt")))
        .set(attrs)
        .build();
  }

  @Override
  public void save(final Session session) {
    Map<String, String> attributes = new HashMap<>(session.attributes());
    attributes.put("_accessedAt", Long.toString(session.accessedAt()));
    attributes.put("_createdAt", Long.toString(session.createdAt()));
    attributes.put("_savedAt", Long.toString(session.savedAt()));

    cache.put(new Element(session.id(), attributes));
  }

  @Override
  public void create(final Session session) {
    save(session);
  }

  @Override
  public void delete(final String id) {
    cache.remove(id);
  }

}

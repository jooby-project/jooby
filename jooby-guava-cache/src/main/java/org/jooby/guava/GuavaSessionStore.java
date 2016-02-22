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
package org.jooby.guava;

import static java.util.Objects.requireNonNull;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.jooby.Session;
import org.jooby.Session.Builder;

import com.google.common.cache.Cache;

/**
 * <h1>session store</h1>
 * <p>
 * This module comes with a {@link Session.Store} implementation. In order to use it you need to
 * define a cache named <code>session</code> in your <code>application.conf</code> file:
 * </p>
 *
 * <pre>
 * guava.session = "maximumSize=10"
 * </pre>
 *
 * And set the {@link GuavaSessionStore}:
 *
 * <pre>
 * {
 *   session(GuavaSessionStore.class);
 * }
 * </pre>
 *
 * You can access to the ```session``` via name:
 *
 * <pre>
 * {
 *   get("/", req {@literal} {
 *     Cache cache = req.require("session", Cache.class);
 *   });
 * }
 * </pre>
 *
 * @author edgar
 *
 */
@Singleton
public class GuavaSessionStore implements Session.Store {

  private Cache<String, Session> cache;

  @Inject
  public GuavaSessionStore(@Named("session") final Cache<String, Session> cache) {
    this.cache = requireNonNull(cache, "Session cache is required.");
  }

  @Override
  public Session get(final Builder builder) {
    return cache.getIfPresent(builder.sessionId());
  }

  @Override
  public void save(final Session session) {
    cache.put(session.id(), session);
  }

  @Override
  public void create(final Session session) {
    cache.put(session.id(), session);
  }

  @Override
  public void delete(final String id) {
    cache.invalidate(id);
  }

}

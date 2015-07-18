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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.internal;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jooby.Cookie;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Session;
import org.jooby.Session.Definition;
import org.jooby.internal.reqparam.ParserExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

@Singleton
public class SessionManager {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Session.Store store;

  private final Cookie.Definition template;

  private final String secret;

  private final long saveInterval;

  private final ParserExecutor resolver;

  private final long timeout;

  @Inject
  public SessionManager(final Config config, final Definition def, final Session.Store store,
      final ParserExecutor resolver) {
    this.store = store;
    this.resolver = resolver;
    this.secret = config.hasPath("application.secret")
        ? config.getString("application.secret")
        : null;

    Config $session = config.getConfig("session");

    // save interval
    this.saveInterval = def.saveInterval()
        .orElse($session.getDuration("saveInterval", TimeUnit.MILLISECONDS));

    // build cookie
    Cookie.Definition source = def.cookie();

    this.template = new Cookie.Definition(source);

    template.name(source.name().orElse($session.getString("cookie.name")));

    if (!template.comment().isPresent() && $session.hasPath("cookie.comment")) {
      template.comment($session.getString("cookie.comment"));
    }
    if (!template.domain().isPresent() && $session.hasPath("cookie.domain")) {
      template.domain($session.getString("cookie.domain"));
    }
    template.httpOnly(
        source.httpOnly()
            .orElse($session.getBoolean("cookie.httpOnly"))
        );

    template.maxAge(
        source.maxAge()
            .orElse(seconds($session, "cookie.maxAge"))
        );
    template.path(source.path()
        .orElse($session.getString("cookie.path"))
        );
    template.secure(source.secure()
        .orElse($session.getBoolean("cookie.secure"))
        );

    this.timeout = TimeUnit.SECONDS.toMillis(template.maxAge().get());
  }

  public Session create(final Request req, final Response rsp) {
    Session session = new SessionImpl.Builder(resolver, true, store.generateID(), timeout).build();
    log.debug("session created: {}", session);
    Cookie.Definition cookie = cookie(session);
    log.debug("  new cookie: {}", cookie);
    rsp.cookie(cookie);
    return session;
  }

  public Session get(final Request req, final Response rsp) {
    return req.cookie(template.name().get()).toOptional()
        .map(cookie -> {
          String sessionId = unsign(cookie);
          log.debug("loading session: {}", sessionId);
          Session session = store.get(
              new SessionImpl.Builder(resolver, false, sessionId, timeout)
              );
          if (timeout >= 0 && session != null) {
            Cookie.Definition setCookie = cookie(session);
            log.debug("  touch cookie: {}", setCookie);
            rsp.cookie(setCookie);
          }
          return session;
        }).orElse(null);
  }

  public void destroy(final Session session) {
    log.debug("  deleting: {}", session.id());
    store.delete(session.id());
  }

  public void requestDone(final Session session) {
    try {
      createOrUpdate((SessionImpl) ((RequestScopedSession) session).session());
    } catch (Exception ex) {
      log.error("Unable to create/update HTTP session", ex);
    }
  }

  public Cookie.Definition cookie() {
    return template;
  }

  private void createOrUpdate(final SessionImpl session) {
    session.touch();
    if (session.isNew()) {
      session.aboutToSave();
      store.create(session);
    } else if (session.isDirty()) {
      session.aboutToSave();
      store.save(session);
    } else {
      long now = System.currentTimeMillis();
      long interval = now - session.savedAt();
      if (interval >= saveInterval) {
        session.aboutToSave();
        store.save(session);
      }
    }
    session.markAsSaved();
  }

  private String sign(final String sessionId) {
    return secret == null ? sessionId : Cookie.Signature.sign(sessionId, secret);
  }

  private String unsign(final String sessionId) {
    if (secret == null) {
      return sessionId;
    }
    return Cookie.Signature.unsign(sessionId, secret);
  }

  private Cookie.Definition cookie(final Session session) {
    // set cookie
    return new Cookie.Definition(this.template).value(sign(session.id()));
  }

  private int seconds(final Config $session, final String name) {
    Object value = $session.getAnyRef(name);
    if (value instanceof Number) {
      // it isn't exactly as getDuration. Here the default time unit for number is seconds
      return ((Number) value).intValue();
    }
    return (int) $session.getDuration(name, TimeUnit.SECONDS);
  }

}

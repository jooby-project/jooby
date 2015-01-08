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

import org.jooby.Cookie;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Session;
import org.jooby.Session.Definition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

public class SessionManager {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private final Session.Definition def;

  private final Session.Store store;

  private final Cookie.Definition cookie;

  private final String secret;

  private final long timeout;

  private final long saveInterval;

  public SessionManager(final Config config, final Definition def) {
    this.def = def;
    this.store = this.def.store();
    this.secret = config.hasPath("application.secret")
        ? config.getString("application.secret")
        : null;

    Config $session = config.getConfig("application.session");

    // timeout
    this.timeout = def.timeout().orElse(duration($session, "timeout", TimeUnit.SECONDS));

    // save interval
    this.saveInterval = def.saveInterval()
        .orElse(duration($session, "saveInterval", TimeUnit.SECONDS));

    // build cookie
    Cookie.Definition source = def.cookie();

    this.cookie = new Cookie.Definition(source);

    cookie.name(source.name().orElse($session.getString("cookie.name")));

    if (!cookie.comment().isPresent() && $session.hasPath("cookie.comment")) {
      cookie.comment($session.getString("cookie.comment"));
    }
    if (!cookie.domain().isPresent() && $session.hasPath("cookie.domain")) {
      cookie.domain($session.getString("cookie.domain"));
    }
    cookie.httpOnly(source.httpOnly().orElse($session.getBoolean("cookie.httpOnly")));
    cookie.maxAge(source.maxAge()
        .orElse(duration($session, "cookie.maxAge", TimeUnit.SECONDS))
        );
    cookie.path(source.path()
        .orElse($session.getString("cookie.path"))
        );
    cookie.secure(source.secure()
        .orElse($session.getBoolean("cookie.secure"))
        );
  }

  public Session create(final Request req, final Response rsp) {
    Session session = new SessionImpl.Builder(true, store.generateID(), timeout)
        .build();
    log.debug("session created: {}", session);
    // set cookie
    Cookie.Definition cookie = new Cookie.Definition(this.cookie)
        .value(sign(session.id()));
    log.debug("setting cookie: {}", cookie);
    rsp.cookie(cookie);
    return session;
  }

  public Session get(final Request req) {
    SessionImpl session = req.cookie(cookie.name().get())
        .map(cookie -> {
          log.debug("session cookie found: {}", cookie);
          return (SessionImpl) store.get(
              new SessionImpl.Builder(false, unsign(cookie.value().get()), timeout)
              );
        }).orElse(null);

    if (session != null && !session.validate()) {
      destroy(session);
      return null;
    }
    return session;
  }

  public void destroy(final Session session) {
    store.delete(session.id());
  }

  public void requestDone(final Session session) {
    createOrUpdate((SessionImpl) ((RequestScopedSession) session).session);
  }

  public Cookie.Definition cookie() {
    return cookie;
  }

  private void createOrUpdate(final SessionImpl session) {
    session.touch();
    if (session.isNew()) {
      store.create(session);
    } else if (session.isDirty()) {
      store.save(session);
    } else if ((System.currentTimeMillis() - session.savedAt()) >= saveInterval) {
      store.save(session);
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

  private static long duration(final Config config, final String name, final TimeUnit unit) {
    try {
      return config.getLong(name);
    } catch (ConfigException.WrongType ex) {
      return config.getDuration(name, unit);
    }
  }

}

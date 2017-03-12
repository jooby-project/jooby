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
package org.jooby.internal;

import org.jooby.Cookie;
import org.jooby.Cookie.Definition;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.internal.parser.ParserExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Save session data in a cookie.
 *
 * @author edgar
 */
public class CookieSessionManager implements SessionManager {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(SessionManager.class);

  private final ParserExecutor resolver;

  private Definition cookie;

  private long timeout;

  private String secret;

  @Inject
  public CookieSessionManager(final ParserExecutor resolver, final Session.Definition cookie,
      @Named("application.secret") final String secret) {
    this.resolver = resolver;
    this.cookie = cookie.cookie();
    this.timeout = TimeUnit.SECONDS.toMillis(this.cookie.maxAge().get());
    this.secret = secret;
  }

  @Override
  public Session create(final Request req, final Response rsp) {
    Session session = new SessionImpl.Builder(resolver, true, Session.COOKIE_SESSION, -1).build();
    log.debug("session created: {}", session);
    rsp.after(saveCookie());
    return session;
  }

  @Override
  public Session get(final Request req, final Response rsp) {
    return req.cookie(cookie.name().get()).toOptional().map(raw -> {
      SessionImpl.Builder session = new SessionImpl.Builder(resolver, false, Session.COOKIE_SESSION,
          -1);
      Map<String, String> attributes = attributes(raw);
      session.set(attributes);
      rsp.after(saveCookie());
      return session.build();
    }).orElse(null);
  }

  @Override
  public void destroy(final Session session) {
    // NOOP
  }

  @Override
  public void requestDone(final Session session) {
    // NOOP
  }

  @Override
  public Definition cookie() {
    return new Definition(cookie);
  }

  private Map<String, String> attributes(final String raw) {
    String unsigned = Cookie.Signature.unsign(raw, secret);
    return Cookie.URL_DECODER.apply(unsigned);
  }

  private Route.After saveCookie() {
    return (req, rsp, result) -> {
      req.ifSession().ifPresent(session -> {
        Optional<String> value = req.cookie(cookie.name().get()).toOptional();
        Map<String, String> initial = value
            .map(this::attributes)
            .orElse(Collections.emptyMap());
        Map<String, String> attributes = session.attributes();
        // is dirty?
        boolean dirty = !initial.equals(attributes);
        log.debug("session dirty: {}", dirty);
        if (dirty) {
          log.debug("saving session cookie");
          String encoded = Cookie.URL_ENCODER.apply(attributes);
          String signed = Cookie.Signature.sign(encoded, secret);
          rsp.cookie(new Cookie.Definition(cookie).value(signed));
        } else if (timeout > 0) {
          // touch session
          value.ifPresent(raw -> rsp.cookie(new Cookie.Definition(cookie).value(raw)));
        }
      });
      return result;
    };
  }

}

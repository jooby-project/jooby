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

import java.util.Map;

import org.jooby.Mutant;
import org.jooby.Response;
import org.jooby.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestScopedSession implements Session {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(Session.class);

  private SessionManager sm;

  private Response rsp;

  private Session session;

  private Runnable resetSession;

  public RequestScopedSession(final SessionManager sm, final Response rsp,
      final Session session, final Runnable resetSession) {
    this.sm = sm;
    this.rsp = rsp;
    this.session = session;
    this.resetSession = resetSession;
  }

  @Override
  public String id() {
    return session.id();
  }

  @Override
  public long createdAt() {
    return session.createdAt();
  }

  @Override
  public long accessedAt() {
    return session.accessedAt();
  }

  @Override
  public long savedAt() {
    return session.savedAt();
  }

  @Override
  public long expiryAt() {
    return session.expiryAt();
  }

  @Override
  public Mutant get(final String name) {
    return session.get(name);
  }

  @Override
  public Map<String, String> attributes() {
    return session.attributes();
  }

  @Override
  public Session set(final String name, final String value) {
    session.set(name, value);
    return this;
  }

  @Override
  public boolean isSet(final String name) {
    return session.isSet(name);
  }

  @Override
  public Mutant unset(final String name) {
    return session.unset(name);
  }

  @Override
  public Session unset() {
    session.unset();
    return this;
  }

  @Override
  public void destroy() {
    if (this.session != null) {
      // clear attributes
      log.debug("destroying session: {}", session.id());
      session.destroy();
      // reset req session
      resetSession.run();
      // clear cookie
      org.jooby.Cookie.Definition cookie = sm.cookie();
      log.debug("  removing cookie: {}", cookie);
      rsp.clearCookie(cookie.name().get());
      // destroy session from storage
      sm.destroy(session);

      // null everything
      this.resetSession = null;
      this.rsp = null;
      this.session = null;
      this.sm = null;
    }
  }

  @Override
  public String toString() {
    return session.toString();
  }

  public Session session() {
    return session;
  }
}

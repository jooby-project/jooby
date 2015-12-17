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
package org.jooby.internal.pac4j;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Session;
import org.jooby.pac4j.Auth;
import org.jooby.pac4j.AuthStore;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthLogout implements Route.Handler {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private String redirectTo;

  public AuthLogout(final String redirectTo) {
    this.redirectTo = requireNonNull(redirectTo, "RedirectTo is required.");
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handle(final Request req, final Response rsp) throws Exception {
    // DON'T create a session for JWT/param/header auth (a.k.a stateless)
    Optional<Session> ifSession = req.ifSession();
    if (ifSession.isPresent()) {
      Optional<String> profileId = ifSession.get().unset(Auth.ID).toOptional();
      if (profileId.isPresent()) {
        Optional<UserProfile> profile = req.require(AuthStore.class).unset(profileId.get());
        log.debug("logout {}", profile);
      }
    } else {
      log.debug("nothing to logout from session");
    }
    String redirectTo = req.<String> get("auth.logout.redirectTo").orElse(this.redirectTo);
    rsp.redirect(redirectTo);
  }

}

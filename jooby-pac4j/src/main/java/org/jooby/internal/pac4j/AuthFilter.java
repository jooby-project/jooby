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
import org.jooby.Route.Chain;
import org.jooby.pac4j.Auth;
import org.jooby.pac4j.AuthStore;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthFilter implements Route.Filter {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Class<? extends Client<?, ?>> clientType;

  private Class<? extends UserProfile> profileType;

  private AuthStore<UserProfile> store;

  public AuthFilter(final Class<? extends Client<?, ?>> clientType,
      final Class<? extends UserProfile> profileType,
      final AuthStore<UserProfile> store) {
    this.clientType = requireNonNull(clientType, "ClientType is required.");
    this.profileType = requireNonNull(profileType, "ProfileType is required.");
    this.store = requireNonNull(store, "Auth store is required.");
  }

  @Override
  public void handle(final Request req, final Response rsp, final Chain chain) throws Exception {
    Optional<String> profileId = req.session().get(Auth.ID).toOptional();
    Optional<UserProfile> profile = profileId.isPresent()
        ? store.get(profileId.get())
        : Optional.empty();

    if (profile.isPresent()) {
      UserProfile userprofile = profile.get();
      req.set(profileType, userprofile);
      log.debug("profile found: {}", userprofile);
      chain.next(req, rsp);
    } else {
      log.debug("profile not found");
      req.session().set(Pac4jConstants.REQUESTED_URL, req.path());

      WebContext ctx = req.require(WebContext.class);
      Clients clients = req.require(Clients.class);
      Client<?, ?> client = clients.findClient(clientType);

      try {
        client.redirect(ctx, true, req.xhr());
        rsp.end();
      } catch (RequiresHttpAction ex) {
        new AuthResponse(rsp).handle(client, ex);
      }
    }
  }

}

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

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Chain;
import org.jooby.Session;
import org.jooby.pac4j.Auth;
import org.jooby.pac4j.AuthStore;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class AuthCallback implements Route.Filter {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private AuthStore store;

  private Clients clients;

  @Inject
  public AuthCallback(final Clients clients, final AuthStore store) {
    this.clients = requireNonNull(clients, "Clients are required.");
    this.store = requireNonNull(store, "Auth store is required.");
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handle(final Request req, final Response rsp, final Chain chain) throws Exception {
    WebContext ctx = req.require(WebContext.class);

    List<Client> clientList = clients.findAllClients();
    Client client = clientList.size() == 1
        ? clientList.get(0)
        : clients.findClient(ctx);

    log.debug("client : {}", client);

    try {
      Credentials credentials = client.getCredentials(ctx);
      log.debug("credentials : {}", credentials);

      // get user profile
      final UserProfile profile = client.getUserProfile(credentials, ctx);
      log.debug("profile : {}", profile);

      Session session = req.session();
      if (profile != null) {
        session.set(Auth.ID, profile.getId());
        store.set(profile);
      }

      // redirect to saved URL
      Optional<String> requestedUrl = session.unset(Pac4jConstants.REQUESTED_URL).toOptional();
      rsp.redirect(requestedUrl.orElse("/"));
    } catch (final RequiresHttpAction ex) {
      new AuthResponse(rsp).handle(client, ex);
    }
  }
}

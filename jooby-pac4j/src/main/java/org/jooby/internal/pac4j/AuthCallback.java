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

import com.google.common.base.Strings;
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
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.*;

@SuppressWarnings("rawtypes")
public class AuthCallback implements Route.Filter {

  /**
   * The logging system.
   */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private AuthStore store;

  private Clients clients;

  private Optional<String> redirectTo;

  @Inject
  public AuthCallback(final Clients clients, final AuthStore store,
      @Named("auth.login.redirectTo") final String redirectTo) {
    this.clients = requireNonNull(clients, "Clients are required.");
    this.store = requireNonNull(store, "Auth store is required.");
    this.redirectTo = Optional.ofNullable(Strings.emptyToNull(redirectTo));
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handle(final Request req, final Response rsp, final Chain chain) throws Throwable {
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
      final CommonProfile profile = client.getUserProfile(credentials, ctx);
      log.debug("profile : {}", profile);

      Session session = req.session();
      if (profile != null) {
        String id = profile.getId();
        req.set(Auth.ID, id);
        session.set(Auth.ID, id);
        store.set(profile);
      }

      // Where to go?
      String requestedUrl = redirectTo.orElseGet(() -> {
        return session.unset(Pac4jConstants.REQUESTED_URL).value("/");
      });
      log.debug("redirecting to: {}", requestedUrl);
      rsp.redirect(requestedUrl);
    } catch (final HttpAction ex) {
      new AuthResponse(rsp).handle(client, ex);
    }
  }
}

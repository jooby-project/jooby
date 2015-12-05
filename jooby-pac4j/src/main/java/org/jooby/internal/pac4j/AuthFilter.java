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

import org.jooby.Err;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Chain;
import org.jooby.Status;
import org.jooby.pac4j.Auth;
import org.jooby.pac4j.AuthStore;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.ClientFinder;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.DirectClient;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;

public class AuthFilter implements Route.Filter {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Class<? extends Client<?, ?>> clientType;

  private String clientName;

  private Class<? extends UserProfile> profileType;

  public AuthFilter(final Class<? extends Client<?, ?>> clientType,
      final Class<? extends UserProfile> profileType) {
    this.clientType = requireNonNull(clientType, "ClientType is required.");
    this.clientName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL,
        clientType.getSimpleName().replace("Client", ""));
    this.profileType = requireNonNull(profileType, "ProfileType is required.");
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  @Override
  public void handle(final Request req, final Response rsp, final Chain chain) throws Exception {
    WebContext ctx = req.require(WebContext.class);

    ClientFinder finder = req.require(ClientFinder.class);
    AuthStore<UserProfile> store = req.require(AuthStore.class);
    Clients clients = req.require(Clients.class);
    Client client = find(finder, clients, ctx, clientType, clientName);

    boolean useSession = client instanceof IndirectClient;

    String profileId = profileID(useSession, req);
    UserProfile profile = profileId == null ? null : store.get(profileId).orElse(null);

    if (profile == null) {
      if (client instanceof DirectClient) {
        log.debug("Performing authentication for client: {}", client);
        try {
          Credentials credentials = client.getCredentials(ctx);
          log.debug("credentials: {}", credentials);
          profile = client.getUserProfile(credentials, ctx);
          log.debug("profile: {}", profile);
          if (profile != null) {
            store.set(profile);
          }
        } catch (final RequiresHttpAction e) {
          throw new TechnicalException("Unexpected HTTP action", e);
        }
      }
    }

    if (profile == null) {
      if (useSession) {
        // indirect client, start authentication
        try {
          final String requestedUrl = ctx.getFullRequestURL();
          log.debug("requestedUrl: {}", requestedUrl);
          ctx.setSessionAttribute(Pac4jConstants.REQUESTED_URL, requestedUrl);
          client.redirect(ctx, true);
          rsp.end();
        } catch (RequiresHttpAction ex) {
          new AuthResponse(rsp).handle(client, ex);
        }
      } else {
        throw new Err(Status.UNAUTHORIZED);
      }
    } else {
      log.debug("profile found: {}", profile);
      seed(req, profileType, profile);
      chain.next(req, rsp);
    }
  }

  private String profileID(final boolean useSession, final Request req) {
    return req.<String> get(Auth.ID)
        .orElseGet(() -> useSession ? req.session().get(Auth.ID).value(null) : null);
  }

  @SuppressWarnings("rawtypes")
  private Client find(final ClientFinder finder, final Clients clients, final WebContext ctx,
      final Class<? extends Client<?, ?>> clientType, final String clientName) {
    List<Client> result = finder.find(clients, ctx, clientName);
    if (result.size() > 0) {
      return result.get(0);
    }
    return clients.findClient(clientType);
  }

  @SuppressWarnings("rawtypes")
  private void seed(final Request req, Class type, final Object profile) {
    while (type != Object.class) {
      req.set(type, profile);
      type = type.getSuperclass();
    }
  }

}

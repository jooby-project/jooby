/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.pac4j;

import javaslang.CheckedFunction1;
import org.jooby.*;
import org.jooby.pac4j.Auth;
import org.jooby.pac4j.AuthStore;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.DirectClient;
import org.pac4j.core.client.IndirectClient;
import org.pac4j.core.client.finder.ClientFinder;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.*;

public class AuthFilter implements Route.Handler {

  @SuppressWarnings("rawtypes")
  private static final Predicate<Client> useSession = c -> c instanceof IndirectClient;

  /**
   * The logging system.
   */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private String clientName;

  private Class<? extends CommonProfile> profileType;

  public AuthFilter(final Class<? extends Client<?, ?>> clientType,
      final Class<? extends CommonProfile> profileType) {
    this.clientName = clientType.getSimpleName();
    this.profileType = requireNonNull(profileType, "ProfileType is required.");
  }

  public AuthFilter setName(final String clientName) {
    this.clientName += Pac4jConstants.ELEMENT_SEPRATOR + clientName;
    return this;
  }

  public String getName() {
    return clientName;
  }

  @SuppressWarnings({"unchecked"})
  @Override
  public void handle(final Request req, final Response rsp) throws Throwable {
    Clients clients = req.require(Clients.class);
    String clientName = req.param(clients.getClientNameParameter()).value(this.clientName);

    WebContext ctx = req.require(WebContext.class);
    ClientFinder finder = req.require(ClientFinder.class);
    AuthStore<CommonProfile> store = req.require(AuthStore.class);

    // stateless or previously authenticated stateful
    CommonProfile profile = find(finder, clients, ctx, null, clientName, client -> {
      String profileId = profileID(useSession.test(client), req);
      CommonProfile identity = profileId == null ? null : store.get(profileId).orElse(null);

      if (identity == null) {
        if (client instanceof DirectClient) {
          log.debug("Performing authentication for client: {}", client);
          try {
            Credentials credentials = client.getCredentials(ctx);
            log.debug("credentials: {}", credentials);
            identity = client.getUserProfile(credentials, ctx);
            log.debug("profile: {}", identity);
            if (identity != null) {
              req.set(Auth.ID, identity.getId());
              req.set(Auth.CNAME, client.getName());
              store.set(identity);
            }
          } catch (HttpAction e) {
            throw new TechnicalException("Unexpected HTTP action", e);
          }
        }
      }
      return identity;
    });

    if (profile == null) {
      // try stateful auth
      Boolean redirected = find(finder, clients, ctx, null, clientName, client -> {
        if (useSession.test(client)) {
          // indirect client, start authentication
          try {
            String queryString = req.queryString().map(it -> "?" + it).orElse("");
            final String requestedUrl = req.path() + queryString;
            log.debug("requestedUrl: {}", requestedUrl);
            ctx.setSessionAttribute(Pac4jConstants.REQUESTED_URL, requestedUrl);
            client.redirect(ctx);
            rsp.end();
          } catch (HttpAction ex) {
            new AuthResponse(rsp).handle(client, ex);
          }
          return Boolean.TRUE;
        } else {
          return null;
        }
      });
      if (redirected != Boolean.TRUE) {
        throw new Err(Status.UNAUTHORIZED);
      }
    } else {
      log.debug("profile found: {}", profile);
      seed(req, profileType, profile);
    }
  }

  private String profileID(final boolean useSession, final Request req) {
    return req.<String>ifGet(Auth.ID)
        .orElseGet(() -> useSession ? req.session().get(Auth.ID).value(null) : null);
  }

  @SuppressWarnings("rawtypes")
  private <T> T find(final ClientFinder finder, final Clients clients, final WebContext ctx,
      final Class<? extends Client<?, ?>> clientType, final String clientName,
      final CheckedFunction1<Client, T> fn) throws Throwable {

    List<Client> result = finder.find(clients, ctx, clientName);
    for (Client client : result) {
      T value = fn.apply(client);
      if (value != null) {
        return value;
      }
    }

    return null;
  }

  @SuppressWarnings("rawtypes")
  private void seed(final Request req, Class type, final Object profile) {
    while (type != Object.class) {
      req.set(type, profile);
      type = type.getSuperclass();
    }
  }

}

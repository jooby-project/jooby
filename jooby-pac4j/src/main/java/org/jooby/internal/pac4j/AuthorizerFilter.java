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

import java.util.Map;

import org.jooby.Err;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Status;
import org.pac4j.core.authorization.AuthorizationChecker;
import org.pac4j.core.authorization.Authorizer;
import org.pac4j.core.config.Config;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizerFilter implements Route.Handler {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private String authorizer;

  public AuthorizerFilter(final String authorizer) {
    this.authorizer = requireNonNull(authorizer, "Authorizer's name is required.");
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void handle(final Request req, final Response rsp) throws Exception {
    UserProfile user = req.require(UserProfile.class);
    Config config = req.require(Config.class);
    WebContext ctx = req.require(WebContext.class);
    AuthorizationChecker authorizationChecker = req.require(AuthorizationChecker.class);
    Map<String, Authorizer> authorizers = config.getAuthorizers();
    log.debug("checking access for: {}", user);
    if (!authorizationChecker.isAuthorized(ctx, user, this.authorizer, authorizers)) {
      log.debug("forbidden: {}", user);
      throw new Err(Status.FORBIDDEN);
    }
    log.debug("authorized: {}", user);
  }

}

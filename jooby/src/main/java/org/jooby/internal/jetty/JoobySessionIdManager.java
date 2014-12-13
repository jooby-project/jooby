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
package org.jooby.internal.jetty;

import static java.util.Objects.requireNonNull;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.jooby.Cookie;
import org.jooby.Session.Store;

public class JoobySessionIdManager extends HashSessionIdManager {

  private Store generator;

  private String secret;

  public JoobySessionIdManager(final Store generator, final String secret) {
    this.generator = requireNonNull(generator, "A ID generator is required.");
    this.secret = secret;
  }

  @Override
  public String newSessionId(final long seedTerm) {
    // generate ID.
    String id = Optional.ofNullable(generator.generateID(seedTerm))
        .orElse(super.newSessionId(seedTerm));

    if (secret == null) {
      return id;
    }
    try {
      return Cookie.Signature.sign(id, secret);
    } catch (InvalidKeyException | NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Can't sign session ID", ex);
    }
  }

}

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
package org.jooby.pac4j;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;

import org.jooby.Session;
import org.jooby.internal.pac4j.AuthSerializer;
import org.pac4j.core.profile.CommonProfile;

/**
 * An {@link AuthStore} on top of the {@link Session}. This is the default {@link AuthStore}.
 *
 * @author edgar
 * @since 0.6.0
 * @param <U> User profile to work with.
 */
public class AuthSessionStore<U extends CommonProfile> implements AuthStore<U> {

  public final static String USER_PROFILE = "pac4jUserProfile";

  private Provider<Session> session;

  @Inject
  public AuthSessionStore(final Provider<Session> session) {
    this.session = requireNonNull(session, "Session is required.");
  }

  @Override
  public Optional<U> get(final String id) throws Exception {
    Session session = this.session.get();
    return get(session.get(key(id)).toOptional());
  }

  @Override
  public void set(final U profile) throws Exception {
    this.session.get().set(key(profile.getId()), AuthSerializer.objToStr(profile));
  }

  @Override
  public Optional<U> unset(final String id) throws Exception {
    Session session = this.session.get();
    return get(session.unset(key(id)).toOptional());
  }

  @SuppressWarnings("unchecked")
  private Optional<U> get(final Optional<String> value) {
    U profile = null;
    if (value.isPresent()) {
      profile = (U) AuthSerializer.strToObject(value.get());
    }
    return Optional.ofNullable(profile);
  }

  private static String key(final String property) {
    return key(USER_PROFILE, property);
  }

  private static String key(final String prefix, final String property) {
    return prefix + "." + property;
  }
}

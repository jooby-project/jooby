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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.jooby.Session;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.profile.UserProfile;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;

/**
 * An {@link AuthStore} on top of the {@link Session}. This is the default {@link AuthStore}.
 *
 * @author edgar
 * @since 0.6.0
 */
public class AuthSessionStore implements AuthStore<UserProfile> {

  private static final String SEP = "__;_;";

  private static final String CLASS = "class";

  private static final String REMEMBERED = "remembered";

  private static final String PERMISSIONS = "permissions";

  private static final String ROLES = "roles";

  private static final Set<String> SPECIAL_PROPERTIES = ImmutableSet.of(CLASS, REMEMBERED,
      PERMISSIONS);

  private Provider<Session> session;

  @Inject
  public AuthSessionStore(final Provider<Session> session) {
    this.session = requireNonNull(session, "Session is required.");
  }

  @Override
  public Optional<UserProfile> get(final String id) throws Exception {
    Session session = this.session.get();
    Map<String, String> attributes = session.attributes();
    String prefix = key(id);
    String classKey = key(prefix, CLASS);
    String classname = attributes.get(classKey);
    if (classname == null) {
      return Optional.empty();
    }
    UserProfile profile = (UserProfile) getClass().getClassLoader().loadClass(classname)
        .newInstance();
    attributes.forEach((k, v) -> {
      if (k.startsWith(prefix)) {
        String key = k.substring(prefix.length() + 1);
        if (!SPECIAL_PROPERTIES.contains(key)) {
          profile.addAttribute(key, v);
        }
      }
    });
    profile.setRemembered(session.get(key(prefix, REMEMBERED)).booleanValue());
    profile.setId(id);
    profile.addPermissions(
        Splitter.on(SEP).splitToList(session.get(key(prefix, PERMISSIONS)).value()));
    profile.addRoles(
        Splitter.on(SEP).splitToList(session.get(key(prefix, ROLES)).value()));
    return Optional.of(profile);
  }

  @Override
  public void set(final UserProfile profile) {
    Session session = this.session.get();
    Map<String, Object> attributes = profile.getAttributes();
    String prefix = key(profile.getId());
    attributes.forEach((k, v) -> session.set(key(prefix, k), v.toString()));
    session.set(key(prefix, REMEMBERED), profile.isRemembered());
    session.set(key(prefix, CLASS), profile.getClass().getName());
    session.set(key(prefix, PERMISSIONS), Joiner.on(SEP).join(profile.getPermissions()));
    session.set(key(prefix, ROLES), Joiner.on(SEP).join(profile.getRoles()));
  }

  @Override
  public Optional<UserProfile> unset(final String id) throws Exception {
    Session session = this.session.get();
    Optional<UserProfile> profile = get(id);
    String prefix = key(id);
    Map<String, String> attributes = session.attributes();
    attributes.forEach((k, v) -> {
      if (k.startsWith(prefix)) {
        session.unset(k);
      }
    });
    return profile;
  }

  private static String key(final String property) {
    return key(Pac4jConstants.USER_PROFILE, property);
  }

  private static String key(final String prefix, final String property) {
    return prefix + "." + property;
  }
}

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

import java.util.Optional;

import org.pac4j.core.profile.CommonProfile;

/**
 * Contract for saving and restoring {@link CommonProfile}.
 *
 * @author edgar
 * @param <U> User profile to work with.
 */
public interface AuthStore<U extends CommonProfile> {

  /**
   * Call it after a successful authentication in order to restore an {@link CommonProfile}. If
   * profile is present, no authentication is required because user was already authenticated.
   *
   * @param id ID of the profile to restore.
   * @return An {@link CommonProfile}.
   * @throws Exception If restore fails.
   */
  Optional<U> get(String id) throws Exception;

  /**
   * Call it after a successful authentication in order to store an {@link CommonProfile}. The user
   * was successfully authenticated and we want to save it somewhere.
   *
   * @param profile Profile to store.
   * @throws Exception If store fails.
   */
  void set(U profile) throws Exception;

  /**
   * Call it on logout in order to remove an {@link CommonProfile} from the store.
   *
   * @param id ID of the profile to remove.
   * @throws Exception If store fails.
   */
  Optional<U> unset(String id) throws Exception;

}

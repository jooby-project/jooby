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
package org.jooby;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * <h1>service registry</h1>
 * <p>
 * Provides access to services registered by modules or application. The registry is powered by
 * Guice.
 * </p>
 *
 * @author edgar
 * @since 1.0.0.CR3
 */
public interface Registry {

  /**
   * Request a service of the given type.
   *
   * @param type A service type.
   * @param <T> Service type.
   * @return A ready to use object.
   */
  default <T> T require(final Class<T> type) {
    return require(Key.get(type));
  }

  /**
   * Request a service of the given type and name.
   *
   * @param name A service name.
   * @param type A service type.
   * @param <T> Service type.
   * @return A ready to use object.
   */
  default <T> T require(final String name, final Class<T> type) {
    return require(Key.get(type, Names.named(name)));
  }

  /**
   * Request a service of the given type.
   *
   * @param type A service type.
   * @param <T> Service type.
   * @return A ready to use object.
   */
  default <T> T require(final TypeLiteral<T> type) {
    return require(Key.get(type));
  }

  /**
   * Request a service of the given key.
   *
   * @param key A service key.
   * @param <T> Service type.
   * @return A ready to use object.
   */
  <T> T require(Key<T> key);

}

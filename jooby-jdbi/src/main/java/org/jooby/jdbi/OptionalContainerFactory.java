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
package org.jooby.jdbi;

import java.util.Optional;

import org.skife.jdbi.v2.ContainerBuilder;
import org.skife.jdbi.v2.tweak.ContainerFactory;

class OptionalContainerFactory implements ContainerFactory<Optional<?>> {

  @Override
  public boolean accepts(final Class<?> type) {
    return Optional.class.isAssignableFrom(type);
  }

  @Override
  public ContainerBuilder<Optional<?>> newContainerBuilderFor(final Class<?> type) {
    return optional();
  }

  private static ContainerBuilder<Optional<?>> optional() {
    return new ContainerBuilder<Optional<?>>() {

      private Optional<?> value = Optional.empty();

      @Override
      public Optional<?> build() {
        return value;
      }

      @Override
      public ContainerBuilder<Optional<?>> add(final Object it) {
        value = Optional.ofNullable(it);
        return this;
      }
    };
  }



}

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
package org.jooby.internal.camel;

import static java.util.Objects.requireNonNull;

import javax.inject.Inject;

import org.apache.camel.spi.Injector;

public class GuiceInjector implements Injector {

  private com.google.inject.Injector guice;

  @Inject
  public GuiceInjector(final com.google.inject.Injector guice) {
    this.guice = requireNonNull(guice, "An injector is required.");
  }

  @Override
  public <T> T newInstance(final Class<T> type) {
    return guice.getInstance(type);
  }

  @Override
  public <T> T newInstance(final Class<T> type, final Object instance) {
    return newInstance(type);
  }

}

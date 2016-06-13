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
package org.jooby.internal;

import java.util.Optional;

import org.jooby.Route;

public class RouteSourceImpl implements Route.Source {

  private Optional<String> declaringClass;

  private int line;

  public RouteSourceImpl(final String declaringClass, final int line) {
    this.declaringClass = Optional.ofNullable(declaringClass);
    this.line = line;
  }

  @Override
  public int line() {
    return line;
  }

  @Override
  public Optional<String> declaringClass() {
    return declaringClass;
  }

  @Override
  public String toString() {
    return declaringClass.orElse("~unknown") + ":" + line;
  }
}

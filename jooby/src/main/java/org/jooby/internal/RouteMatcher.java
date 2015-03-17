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

import java.util.Collections;
import java.util.Map;

public interface RouteMatcher {

  /**
   * @return Current path under test.
   */
  String path();

  /**
   * @return True, if {@link #path()} matches a path pattern.
   */
  boolean matches();

  /**
   * Get path vars from current path. Or empty map if there is none.
   * This method must be invoked after {@link #matches()}.
   *
   * @return Get path vars from current path. Or empty map if there is none.
   */
  default Map<Object, String> vars() {
    return Collections.emptyMap();
  }
}

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
package org.jooby.spec;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Extends {@link org.jooby.Route.Definition} with extra <code>metadata</code>, like parameter
 * types, return type, doc, etc...
 *
 * Useful for exporting routes to something else.
 *
 * @author edgar
 * @since 0.15.0
 */
public interface RouteSpec extends Serializable {

  /**
   * @return Top level doc (a.k.a summary).
   */
  Optional<String> summary();

  /**
   * @return Route name.
   */
  Optional<String> name();

  /**
   * @return Route method.
   */
  String method();

  /**
   * @return Route pattern.
   */
  String pattern();

  /**
   * @return Route doc.
   */
  Optional<String> doc();

  /**
   * @return List all the types this route can consumes, defaults is: {@code * / *}.
   */
  List<String> consumes();

  /**
   * @return List all the types this route can produces, defaults is: {@code * / *}.
   */
  List<String> produces();

  /**
   * @return List of params or empty list.
   */
  List<RouteParam> params();

  /**
   * @return Route response.
   */
  RouteResponse response();
}

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

import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Route param attributes: name, type, etc...
 *
 * @author edgar
 */
public interface RouteParam {

  /**
   * @return Parameter's name.
   */
  String name();

  /**
   * @return Java type.
   */
  Type type();

  /**
   * @return Type of HTTP param.
   */
  RouteParamType paramType();

  /**
   * @return Default value or <code>null</code>
   */
  Object value();

  /**
   * @return Documentation.
   */
  Optional<String> doc();

  /**
   * @return True for optional param. A param is optional when type is {@link Optional} or has a
   *         default value.
   */
  default boolean optional() {
    return type().toString().startsWith(java.util.Optional.class.getName())
        || value() != null;
  }
}
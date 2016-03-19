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
package org.jooby.mongodb;

import org.mongodb.morphia.EntityInterceptor;
import org.mongodb.morphia.annotations.PrePersist;

/**
 * <p>
 * Setup up an {@link EntityInterceptor} on {@link PrePersist} events that generates an incremental
 * ID.
 * </p>
 *
 * Usage:
 * <pre>
 * {
 *   use(new Monphia().with(IdGen.GLOBAL);
 * }
 * </pre>
 * <p>
 * ID must be of type: {@link Long} and annotated with {@link GeneratedValue}:
 * </p>
 * <pre>
 * &#64;Entity
 * public class MyEntity {
 *   &#64;Id &#64;GeneratedValue Long id;
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.13.0
 */
public enum IdGen {
  /**
   * A global unique ID regardless of the entity type.
   */
  GLOBAL {
    @Override
    public String value(final Class<?> mappedClass) {
      return "Global";
    }
  },

  /**
   * A unique ID per entity type.
   */
  LOCAL {
    @Override
    public String value(final Class<?> mappedClass) {
      return mappedClass.getName();
    }
  };

  /**
   * Get an unique value for the given mapped class.
   *
   * @param mappedClass A mapped class.
   * @return Get an unique value for the given mapped class.
   */
  public abstract String value(Class<?> mappedClass);
}

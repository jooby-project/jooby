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
package org.jooby.mvc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines what media types a route can consume. By default a route can consume any type
 * {@code *}/{@code *}.
 * The <code>Content-Type</code> header is used for finding the best {@link org.jooby.Body.Parser}.
 * If there isn't a {@link org.jooby.Body.Parser} a "415 Unsupported Media Type"
 * response will be generated.
 * <pre>
 *   class Resources {
 *
 *     &#64;Consume("application/json")
 *     public void method(&#64;Body MyBody body) {
 *     }
 *   }
 * </pre>
 * @author edgar
 * @since 0.1.0
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Consumes {
  /**
   * @return Media types the route can consume.
   */
  String[] value();
}

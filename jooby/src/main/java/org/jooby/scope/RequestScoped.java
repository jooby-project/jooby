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
package org.jooby.scope;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import javax.inject.Scope;

/**
 * Define a request scoped object. Steps for defining a request scoped object are:
 *
 * <ol>
 * <li>
 *  Bind the object at bootstrap time:
 *  <pre>
 *    binder.bind(MyRequestObject.class).toProvider(() {@literal ->} {
 *      throw new IllegalStateException("Out of scope!");
 *    });
 *  </pre>
 * </li>
 * <li>
 *  Seed the object from a route handler:
 *   <pre>
 *    use("*", req {@literal ->} {
 *      MyRequestObject object = ...;
 *      req.set(MyRequestObject.class, object);
 *    });
 *  </pre>
 * </li>
 * </ol>
 *
 * @author edgar
 * @since 0.5.0
 */
@Scope
@Documented
@Retention(RUNTIME)
public @interface RequestScoped {
}

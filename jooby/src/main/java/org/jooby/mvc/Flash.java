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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jooby.FlashScope;
import org.jooby.Request;

/**
 * <p>
 * Bind a Mvc parameter to a {@link Request#flash(String)} flash attribute.
 * </p>
 *
 * Accessing to flash scope:
 * <pre>
 *   &#64;Path("/r")
 *   class Resources {
 *
 *     &#64;Get
 *     public void method(&#64;Flash Map&lt;String, String&gt; flash) {
 *     }
 *   }
 * </pre>
 *
 * Accessing to a flash attribute:
 * <pre>
 *   &#64;Path("/r")
 *   class Resources {
 *
 *     &#64;Get
 *     public void method(&#64;Flash String success) {
 *     }
 *   }
 * </pre>
 *
 * Accessing to an optional flash attribute:
 * <pre>
 *   &#64;Path("/r")
 *   class Resources {
 *
 *     &#64;Get
 *     public void method(&#64;Flash Optional&lt;String&gt; success) {
 *     }
 *   }
 * </pre>
 *
 * @author edgar
 * @since 1.0.0.CR4
 * @see FlashScope
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER })
public @interface Flash {
}

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
package org.jooby;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Convert a request param (path, query, form) to something else.
 *
 * <h1>Registering a param converter</h1>
 * There are two ways of registering a param converter:
 *
 * <ol>
 * <li>Using the {@link Jooby#param(ParamConverter)} method</li>
 * <li>From a Guice module:
 * <pre>
 *   Multibinder&lt;ParamConverter&gt; pcb = Multibinder
        .newSetBinder(binder, ParamConverter.class);
     pcb.addBinding().to(MyParamConverter.class);
 * </pre>
 * </li>
 * </ol>
 * Param converters are executed in the order they were registered. The first converter that
 * resolved the type: wins!.
 *
 * <h1>Built-in param converters</h1>
 * These are the built-in param converters:
 * <ol>
 * <li>Primitives and String: convert to int, double, char, string, etc...</li>
 * <li>Enums</li>
 * <li>{@link java.util.Date}: It parses a date using the <code>application.dateFormat</code>
 * property.</li>
 * <li>{@link java.time.LocalDate}: It parses a date using the <code>application.dateFormat</code>
 * property.</li>
 * <li>{@link java.util.Locale}</li>
 * <li>Classes with a static method: <code>valueOf</code></li>
 * <li>Classes with a static method: <code>fromName</code></li>
 * <li>Classes with a static method: <code>fromString</code></li>
 * <li>Classes with a public constructor with one <code>String</code> argument</li>
 * <li>It is an Optional&lt;T&gt;, List&lt;T&gt;, Set&lt;T&gt; or SortedSet&lt;T&gt; where T
 * satisfies one of previous rules</li>
 * </ol>
 *
 * @author edgar
 * @see Jooby#param(ParamConverter)
 * @since 0.5.0
 */
public interface ParamConverter {

  /**
   * Param execution context. Provides access to the conversion service thought the
   * {@link #convert(TypeLiteral, Object[])} method.
   *
   * But also let you access to request/app services.
   *
   * @author edgar
   * @since 0.5.0
   */
  interface Context {

    /**
     * Find and return a service using the provided type.
     *
     * @param type A service type.
     * @param <T> Service type.
     * @return Binded service.
     */
    default <T> T require(final Class<T> type) {
      return require(Key.get(type));
    }

    /**
     * Find and return a service using the provided type.
     *
     * @param type A service type.
     * @param <T> Service type.
     * @return Binded service.
     */
    default <T> T require(final TypeLiteral<T> type) {
      return require(Key.get(type));
    }

    /**
     * Find and return a service using the provided key.
     *
     * @param key A key for a service.
     * @param <T> Service type.
     * @return Binded service.
     */
    <T> T require(Key<T> key);

    /**
     * Ask to the next convert to resolve a type.
     *
     * @param type A type to resolve.
     * @param values Raw values. Types is one of two: <code>String</code> or <code>Upload</code>.
     * @return A converted value.
     * @throws Exception If conversion fails.
     */
    Object convert(TypeLiteral<?> type, Object[] values) throws Exception;
  }

  /**
   * <p>
   *  Convert one or more values to the required type. If the converter doesn't support the
   *  required type a call to {@link Context#convert(TypeLiteral, Object[])} must be done.
   * </p>
   *
   * Example:
   * <pre>
   *  ParamConverter converter = (type, values, next) {@literal ->} {
   *    if (type.getRawType() == MyType.class) {
   *      // convert to MyType
   *      return ...;
   *    }
   *    // no luck! move next
   *    return next.convert(type, values);
   *  }
   * </pre>
   *
   * It's also possible to create generic/parameterized types too:
   *
   * <pre>
   *  public class MyType&lt;T&gt; {}
   *
   *  ParamConverter converter = (type, values, ctx) {@literal ->} {
   *    if (type.getRawType() == MyType.class) {
   *      // Creates a new type from current generic type
   *      TypeLiterale&lt;?&gt; paramType = TypeLiteral
   *        .get(((ParameterizedType) toType.getType()).getActualTypeArguments()[0]);
   *
   *      // Ask param converter to resolve the new type.
   *      Object result = next.convert(paramType, values);
   *      return new MyType(result);
   *    }
   *    // no luck! move next
   *    return ctx.convert(type, values);
   *  }
   * </pre>
   *
   *
   * @param type Requested type.
   * @param values Raw values. Types is one of two: <code>String</code> or <code>Upload</code>.
   * @param ctx Execution context.
   * @return A converted value.
   * @throws Exception If conversion fails.
   */
  Object convert(TypeLiteral<?> type, Object[] values, Context ctx) throws Exception;

}

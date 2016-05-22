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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Parse a request param (path, query, form) or body to something else.
 *
 * <h1>Registering a parser</h1>
 * <p>
 * There are two ways of registering a parser:
 * </p>
 *
 * <ol>
 * <li>Using the {@link Jooby#parser(Parser)} method</li>
 * <li>From a Guice module:
 *
 * <pre>
 *   Multibinder&lt;Parser&gt; pcb = Multibinder
        .newSetBinder(binder, Parser.class);
     pcb.addBinding().to(MyParser.class);
 * </pre>
 * </li>
 * </ol>
 * Parsers are executed in the order they were registered. The first converter that resolved the
 * type: wins!.
 *
 * <h1>Built-in parsers</h1> These are the built-in parsers:
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
 * @see Jooby#parser(Parser)
 * @since 0.6.0
 */
public interface Parser {

  /**
   * A parser callback.
   *
   * @author edgar
   *
   * @param <T> Type of data to parse.
   * @since 0.6.0
   */
  interface Callback<T> {

    /**
     * Parse a raw value to something else.
     *
     * @param data Data to parse.
     * @return A parsed value
     * @throws Exception If something goes wrong.
     */
    Object invoke(T data) throws Throwable;

  }

  /**
   * Expose HTTP params from path, query, form url encoded or multipart request as a raw string.
   *
   * @author edgar
   * @since 0.6.0
   */
  interface ParamReference<T> extends Iterable<T> {

    /**
     * @return Parameter name.
     */
    String name();

    /**
     * @return Return the first param or throw {@link Err} with a bad request code when missing.
     */
    T first();

    /**
     * @return Return the last param or throw {@link Err} with a bad request code when missing.
     */
    T last();

    /**
     * Get the param at the given index or throw {@link Err} with a bad request code when missing.
     *
     * @param index Param index.
     * @return Param at the given index or throw {@link Err} with a bad request code when missing.
     */
    T get(int index);

    @Override
    Iterator<T> iterator();

    /**
     * @return Number of values for this parameter.
     */
    int size();

  }

  /**
   * Expose the HTTP body as a series of bytes or text.
   *
   * @author edgar
   * @since 0.6.0
   */
  interface BodyReference {
    /**
     * Returns the HTTP body as a byte array.
     *
     * @return HTTP body as byte array.
     * @throws IOException If reading fails.
     */
    byte[] bytes() throws IOException;

    /**
     * Returns the HTTP body as text.
     *
     * @return HTTP body as text.
     * @throws IOException If reading fails.
     */
    String text() throws IOException;

    /**
     * @return Body length.
     */
    long length();

    /**
     * Write the content to the given output stream. This method won't close the
     * {@link OutputStream}.
     *
     * @param output An output stream.
     * @throws Exception If write fails.
     */
    void writeTo(final OutputStream output) throws Exception;
  }

  /**
   * A parser can be executed against a simply HTTP param, a set of HTTP params, an file
   * {@link Upload} or HTTP {@link BodyReference}.
   *
   * This class provides utility methods for selecting one of the previous source. It is possible to
   * write a parser and apply it against multiple sources, like HTTP param and HTTP body.
   *
   * Here is an example that will parse text to an int, provided as a HTTP param or body:
   *
   * <pre>
   * {
   *   parser((type, ctx) {@literal ->} {
   *     if (type.getRawType() == int.class) {
   *       return ctx
   *           .param(values {@literal ->} Integer.parseInt(values.get(0))
   *           .body(body {@literal ->} Integer.parseInt(body.text()));
   *     }
   *     return ctx.next();
   *   });
   *
   *   get("/", req {@literal ->} {
   *     // use the param strategy
   *     return req.param("p").intValue();
   *   });
   *
   *   post("/", req {@literal ->} {
   *     // use the body strategy
   *     return req.body().intValue();
   *   });
   * }
   * </pre>
   *
   * @author edgar
   * @since 0.6.0
   */
  interface Builder {

    /**
     * Add a HTTP body callback. The Callback will be executed when current context is bound to the
     * HTTP body via {@link Request#body()}.
     *
     * If current {@link Context} isn't a HTTP body a call to {@link Context#next()} is made.
     *
     * @param callback A body parser callback.
     * @return This builder.
     */
    Builder body(Callback<BodyReference> callback);

    /**
     * Like {@link #body(Callback)} but it skip the callback if the requested type is an
     * {@link Optional}.
     *
     * @param callback A body parser callback.
     * @return This builder.
     */
    Builder ifbody(Callback<BodyReference> callback);

    /**
     * Add a HTTP param callback. The Callback will be executed when current context is bound to a
     * HTTP param via {@link Request#param(String)}.
     *
     * If current {@link Context} isn't a HTTP param a call to {@link Context#next()} is made.
     *
     * @param callback A param parser callback.
     * @return This builder.
     */
    Builder param(Callback<ParamReference<String>> callback);

    /**
     * Like {@link #param(Callback)} but it skip the callback if the requested type is an
     * {@link Optional}.
     *
     * @param callback A param parser callback.
     * @return This builder.
     */
    Builder ifparam(Callback<ParamReference<String>> callback);

    /**
     * Add a HTTP params callback. The Callback will be executed when current context is bound to a
     * HTTP params via {@link Request#params()}.
     *
     * If current {@link Context} isn't a HTTP params a call to {@link Context#next()} is made.
     *
     * @param callback A params parser callback.
     * @return This builder.
     */
    Builder params(Callback<Map<String, Mutant>> callback);

    /**
     * Like {@link #params(Callback)} but it skip the callback if the requested type is an
     * {@link Optional}.
     *
     * @param callback A params parser callback.
     * @return This builder.
     */
    Builder ifparams(Callback<Map<String, Mutant>> callback);

    /**
     * Add a HTTP upload callback. The Callback will be executed when current context is bound to a
     * HTTP upload via {@link Request#param(String)}.
     *
     * If current {@link Context} isn't a HTTP upload a call to {@link Context#next()} is made.
     *
     * @param callback A upload parser callback.
     * @return This builder.
     */
    Builder upload(Callback<ParamReference<Upload>> callback);

    /**
     * Like {@link #upload(Callback)} but it skip the callback if the requested type is an
     * {@link Optional}.
     *
     * @param callback A upload parser callback.
     * @return This builder.
     */
    Builder ifupload(Callback<ParamReference<Upload>> callback);
  }

  /**
   * Allows you to access to parsing strategies, content type view {@link #type()} and invoke next
   * parser in the chain via {@link #next()} methods.
   *
   * @author edgar
   * @since 0.6.0
   */
  interface Context extends Builder {

    /**
     * Requires a service with the given type.
     *
     * @param type Service type.
     * @param <T> Service type.
     * @return A service.
     */
    <T> T require(final Class<T> type);

    /**
     * Requires a service with the given type.
     *
     * @param type Service type.
     * @param <T> Service type.
     * @return A service.
     */
    <T> T require(final TypeLiteral<T> type);

    /**
     * Requires a service with the given type.
     *
     * @param key Service key.
     * @param <T> Service type.
     * @return A service.
     */
    <T> T require(final Key<T> key);

    /**
     * Content Type header, if current context was bind to a HTTP body via {@link Request#body()}.
     * If current context was bind to a HTTP param, media type is set to <code>text/plain</code>.
     *
     * @return Current type.
     */
    MediaType type();

    /**
     * Invoke next parser in the chain.
     *
     * @return A parsed value.
     * @throws Exception An err with a 400 status.
     */
    Object next() throws Throwable;

    /**
     * Invoke next parser in the chain and switch/change the target type we are looking for. Useful
     * for generic containers classes, like collections or optional values.
     *
     * @param type A new type to use.
     * @return A parsed value.
     * @throws Exception An err with a 400 status.
     */
    Object next(TypeLiteral<?> type) throws Throwable;

    /**
     * Invoke next parser in the chain and switch/change the target type we are looking for but also
     * the current value. Useful for generic containers classes, like collections or optional
     * values.
     *
     * @param type A new type to use.
     * @param data Data to be parsed.
     * @return A parsed value.
     * @throws Exception An err with a 400 status.
     */
    Object next(TypeLiteral<?> type, Object data) throws Throwable;

  }

  /**
   * <p>
   * Parse one or more values to the required type. If the parser doesn't support the required type
   * a call to {@link Context#next(TypeLiteral, Object)} must be done.
   * </p>
   *
   * Example:
   *
   * <pre>
   *  Parser converter = (type, ctx) {@literal ->} {
   *    if (type.getRawType() == MyType.class) {
   *      // convert to MyType
   *      return ...;
   *    }
   *    // no luck! move next
   *    return next.next();
   *  }
   * </pre>
   *
   * It's also possible to create generic/parameterized types too:
   *
   * <pre>
   *  public class MyContainerType&lt;T&gt; {}
   *
   *  ParamConverter converter = (type, ctx) {@literal ->} {
   *    if (type.getRawType() == MyContainerType.class) {
   *      // Creates a new type from current generic type
   *      TypeLiterale&lt;?&gt; paramType = TypeLiteral
   *        .get(((ParameterizedType) toType.getType()).getActualTypeArguments()[0]);
   *
   *      // Ask param converter to resolve the new/next type.
   *      Object result = next.next(paramType);
   *      return new MyType(result);
   *    }
   *    // no luck! move next
   *    return ctx.next();
   *  }
   * </pre>
   *
   * @param type Requested type.
   * @param ctx Execution context.
   * @return A parsed value.
   * @throws Throwable If conversion fails.
   */
  Object parse(TypeLiteral<?> type, Context ctx) throws Throwable;

}

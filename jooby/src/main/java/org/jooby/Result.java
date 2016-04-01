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

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Utility class for HTTP responses. Usually you start with a result builder {@link Results} and
 * then you customize (or not) one or more HTTP attribute.
 *
 * <p>
 * The following examples build the same output:
 * </p>
 *
 * <pre>
 * {
 *   get("/", (req, rsp) {@literal ->} {
 *     rsp.status(200).send("Something");
 *   });
 *
 *   get("/", req {@literal ->} Results.with("Something", 200);
 * }
 * </pre>
 *
 * A result is also responsible for content negotiation (if required):
 *
 * <pre>
 * {
 *   get("/", () {@literal ->} {
 *     Object model = ...;
 *     return Results.when("text/html", () {@literal ->} Viewable.of("view", "model", model))
 *       .when("application/json", () {@literal ->} model);
 *   });
 * }
 * </pre>
 *
 * <p>
 * The example above will render a view when accept header is "text/html" or just send a text
 * version of model when the accept header is "application/json".
 * </p>
 *
 * @author edgar
 * @since 0.5.0
 * @see Results
 */
public class Result {

  private static Map<String, Object> NO_HEADERS = ImmutableMap.of();

  /** Response headers. */
  private Map<String, Object> headers = NO_HEADERS;

  /** Response status. */
  private Status status;

  /** Response content-type. */
  private MediaType type;

  private final Map<MediaType, Supplier<Object>> data = new LinkedHashMap<>();

  /** Quick access to first result . */
  private Supplier<Object> first;

  /**
   * Set response status.
   *
   * @param status A new response status to use.
   * @return This content.
   */
  public Result status(final Status status) {
    this.status = requireNonNull(status, "A status is required.");
    return this;
  }

  /**
   * Set response status.
   *
   * @param status A new response status to use.
   * @return This content.
   */
  public Result status(final int status) {
    return status(Status.valueOf(status));
  }

  /**
   * Set the content type of this content.
   *
   * @param type A content type.
   * @return This content.
   */
  public Result type(final MediaType type) {
    this.type = requireNonNull(type, "A content type is required.");
    return this;
  }

  /**
   * Set the content type of this content.
   *
   * @param type A content type.
   * @return This content.
   */
  public Result type(final String type) {
    return type(MediaType.valueOf(type));
  }

  /**
   * Set result content.
   *
   * @param content A result content.
   * @return This content.
   */
  public Result set(final Object content) {
    Supplier<Object> supplier = () -> content;
    first = supplier;
    data.put(MediaType.all, supplier);
    return this;
  }

  /**
   * Add a when clause for a custom result for the given media-type.
   *
   * @param type A media type to test for.
   * @param supplier An object supplier.
   * @return This result.
   */
  public Result when(final String type, final Supplier<Object> supplier) {
    return when(MediaType.valueOf(type), supplier);
  }

  /**
   * Add a when clause for a custom result for the given media-type.
   *
   * @param type A media type to test for.
   * @param supplier An object supplier.
   * @return This result.
   */
  public Result when(final MediaType type, final Supplier<Object> supplier) {
    requireNonNull(type, "A media type is required.");
    requireNonNull(supplier, "A supplier fn is required.");
    first = supplier;
    data.put(type, supplier);
    return this;
  }

  /**
   * @return headers for content.
   */
  public Map<String, Object> headers() {
    return headers;
  }

  /**
   * @return Body status.
   */
  public Optional<Status> status() {
    return Optional.ofNullable(status);
  }

  /**
   * @return Body type.
   */
  public Optional<MediaType> type() {
    return Optional.ofNullable(type);
  }

  /**
   * Get a result value.
   *
   * @return Value or <code>empty</code>
   */
  public Optional<Object> ifGet() {
    return ifGet(MediaType.ALL);
  }

  /**
   * Get a result value.
   *
   * @return Value or <code>null</code>
   */
  public <T> T get() {
    return get(MediaType.ALL);
  }

  /**
   * Get a result value for the given types (accept header).
   *
   * @param types Accept header.
   * @return Result content.
   */
  public Optional<Object> ifGet(final List<MediaType> types) {
    return Optional.ofNullable(get(types));
  }

  /**
   * Get a result value for the given types (accept header).
   *
   * @param types Accept header.
   * @return Result content or <code>null</code>.
   */
  @SuppressWarnings("unchecked")
  public <T> T get(final List<MediaType> types) {
    requireNonNull(types, "Types are required.");
    int size = data.size();
    if (size == 1) {
      return (T) first.get();
    }
    if (size == 0) {
      return null;
    }
    Supplier<Object> provider = MediaType
        .matcher(types)
        .first(ImmutableList.copyOf(data.keySet()))
        .map(it -> data.remove(it))
        .orElseThrow(
            () -> new Err(Status.NOT_ACCEPTABLE, Joiner.on(", ").join(types)));
    return (T) provider.get();
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param value Header's value.
   * @return This content.
   */
  public Result header(final String name, final Object value) {
    requireNonNull(name, "Header's name is required.");
    requireNonNull(value, "Header's value is required.");

    put(name, value);
    return this;
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param values Header's values.
   * @return This content.
   */
  public Result header(final String name, final Object... values) {
    requireNonNull(name, "Header's name is required.");
    requireNonNull(values, "Header's values are required.");

    return header(name, ImmutableList.copyOf(values));
  }

  /**
   * Sets a response header with the given name and value. If the header had already been set,
   * the new value overwrites the previous one.
   *
   * @param name Header's name.
   * @param values Header's values.
   * @return This content.
   */
  public Result header(final String name, final Iterable<Object> values) {
    requireNonNull(name, "Header's name is required.");
    requireNonNull(values, "Header's values are required.");

    put(name, values);
    return this;
  }

  @Override
  protected Result clone() {
    Result result = new Result();
    headers.forEach(result::header);
    result.status = status;
    result.type = type;
    result.first = first;
    result.data.putAll(data);
    return result;
  }

  private void put(final String name, final Object val) {
    if (headers == NO_HEADERS) {
      // assign memory
      headers = new LinkedHashMap<>();
    }
    headers.put(name, val);
  }

}

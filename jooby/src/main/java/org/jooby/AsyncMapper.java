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

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.jooby.internal.mapper.CallableMapper;
import org.jooby.internal.mapper.CompletableFutureMapper;

/**
 * <h1>async-mapper</h1>
 * <p>
 * Map {@link Callable} and {@link CompletableFuture} results to {@link Deferred Deferred API}.
 * </p>
 *
 * <h2>usage</h2>
 * <pre>{@code
 * {
 *   map(new AsyncMapper());
 *
 *   get("/callable", () -> {
 *     return new Callable<String> () {
 *       public String call() {
 *         return "OK";
 *       }
 *     };
 *   });
 *
 *   get("/completable-future", () -> {
 *     return CompletableFuture.supplyAsync(() -> "OK");
 *   });
 * }
 * }</pre>
 *
 * @author edgar
 * @since 1.0.0
 */
@SuppressWarnings("rawtypes")
public class AsyncMapper implements Route.Mapper {

  @Override
  public Object map(final Object value) throws Throwable {
    if (value instanceof Callable) {
      return new CallableMapper().map((Callable) value);
    } else if (value instanceof CompletableFuture) {
      return new CompletableFutureMapper().map((CompletableFuture) value);
    }
    return value;
  }

}

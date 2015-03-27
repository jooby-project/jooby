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
package org.jooby.internal.reqparam;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.jooby.Err;
import org.jooby.ParamConverter;
import org.jooby.Request;
import org.jooby.Status;

import com.google.common.collect.ImmutableList;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class ParamResolver {

  private static final Object NOT_FOUND = new Object();

  private List<ParamConverter> converters;

  private Injector injector;


  @Inject
  public ParamResolver(final Injector injector, final Set<ParamConverter> converters) {
    this.injector = requireNonNull(injector, "An injector is required.");
    this.converters = ImmutableList.copyOf(converters);
  }

  public <T> T convert(final TypeLiteral<?> type, final Object value) {
    return convert(type, value == null ? null : new Object[]{value });
  }

  @SuppressWarnings("unchecked")
  public <T> T convert(final TypeLiteral<?> type, final Object[] values) {
    try {
      requireNonNull(type, "A type is required.");
      Object result = ctx(injector, type, converters).convert(type, values);
      if (result == NOT_FOUND) {
        throw new Err(Status.BAD_REQUEST, "No converter for " + type);
      }
      return (T) result;
    } catch (Err err) {
      throw err;
    } catch (Exception ex) {
      throw new Err(Status.BAD_REQUEST, ex);
    }
  }

  private static ParamConverter.Context ctx(final Injector injector,
      final TypeLiteral<?> seed, final List<ParamConverter> converters) {
    return new ParamConverter.Context() {
      int cursor = 0;

      TypeLiteral<?> type = seed;

      @Override
      public Object convert(final TypeLiteral<?> nexttype, final Object[] values)
          throws Exception {
        if (cursor == converters.size()) {
          return NOT_FOUND;
        }
        if (!type.equals(nexttype)) {
          // reset cursor on type changes.
          cursor = 0;
          type = nexttype;
        }
        ParamConverter next = converters.get(cursor);
        cursor += 1;
        Object result = next.convert(nexttype, values, this);
        cursor -= 1;
        return result;
      }

      @Override
      public <T> T require(final Key<T> key) {
        return injector.getInstance(key);
      }

    };
  }
}

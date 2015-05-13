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

import java.util.List;
import java.util.Map;

import org.jooby.Mutant;
import org.jooby.Parser;
import org.jooby.Parser.Builder;
import org.jooby.Parser.Callback;
import org.jooby.Upload;

import com.google.common.collect.ImmutableMap;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;

@SuppressWarnings("rawtypes")
public class ParserBuilder implements Parser.Builder {

  private ImmutableMap.Builder<TypeLiteral<?>, Parser.Callback> strategies = ImmutableMap
      .builder();

  public final TypeLiteral<?> toType;

  private final TypeLiteral<?> type;

  public final Object value;

  private Parser.Context ctx;

  public ParserBuilder(final Parser.Context ctx, final TypeLiteral<?> toType, final Object value) {
    this.ctx = ctx;
    this.toType = toType;
    this.type = typeOf(value);
    this.value = value;
  }

  private TypeLiteral<?> typeOf(final Object value) {
    if (value instanceof List) {
      List values = (List) value;
      if (values.size() > 0) {
        if (values.iterator().next() instanceof Upload) {
          return TypeLiteral.get(Types.listOf(Upload.class));
        }
      }
      return TypeLiteral.get(Types.listOf(String.class));
    } else if (value instanceof Map) {
      return TypeLiteral.get(Types.mapOf(String.class, Mutant.class));
    } else if (value instanceof Parser.BodyReference) {
      return TypeLiteral.get(Parser.BodyReference.class);
    }
    return TypeLiteral.get(value.getClass());
  }

  @Override
  public Builder body(final Callback<Parser.BodyReference> callback) {
    strategies.put(TypeLiteral.get(Parser.BodyReference.class), callback);
    return this;
  }

  @Override
  public Builder param(final Callback<List<String>> callback) {
    strategies.put(TypeLiteral.get(Types.listOf(String.class)), callback);
    return this;
  }

  @Override
  public Builder params(final Callback<Map<String, Mutant>> callback) {
    strategies.put(TypeLiteral.get(Types.mapOf(String.class, Mutant.class)), callback);
    return this;
  }

  @Override
  public Builder upload(final Callback<List<Upload>> callback) {
    strategies.put(TypeLiteral.get(Types.listOf(Upload.class)), callback);
    return this;
  }

  @SuppressWarnings("unchecked")
  public Object parse() throws Exception {
    Map<TypeLiteral<?>, Callback> map = strategies.build();
    Callback callback = map.get(type);
    if (callback == null) {
      return ctx.next(toType, value);
    }
    return callback.invoke(value);
  }

}

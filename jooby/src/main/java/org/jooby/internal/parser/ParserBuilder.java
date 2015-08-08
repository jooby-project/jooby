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
package org.jooby.internal.parser;

import java.util.Map;
import java.util.Optional;

import org.jooby.Mutant;
import org.jooby.Parser;
import org.jooby.Parser.Builder;
import org.jooby.Parser.Callback;
import org.jooby.Upload;
import org.jooby.internal.BodyReferenceImpl;
import org.jooby.internal.StrParamReferenceImpl;
import org.jooby.internal.UploadParamReferenceImpl;

import com.google.common.collect.ImmutableMap;
import com.google.inject.TypeLiteral;

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
    if (value instanceof Map) {
      return TypeLiteral.get(Map.class);
    }
    return TypeLiteral.get(value.getClass());
  }

  @Override
  public Builder body(final Callback<Parser.BodyReference> callback) {
    strategies.put(TypeLiteral.get(BodyReferenceImpl.class), callback);
    return this;
  }

  public Builder ifbody(final Callback<Parser.BodyReference> callback) {
    return body(ifcallback(callback));
  }

  @Override
  public Builder param(final Callback<Parser.ParamReference<String>> callback) {
    strategies.put(TypeLiteral.get(StrParamReferenceImpl.class), callback);
    return this;
  }

  public Builder ifparam(final Callback<Parser.ParamReference<String>> callback) {
    return param(ifcallback(callback));
  }

  @Override
  public Builder params(final Callback<Map<String, Mutant>> callback) {
    strategies.put(TypeLiteral.get(Map.class), callback);
    return this;
  }

  public Builder ifparams(final Callback<Map<String, Mutant>> callback) {
    return params(ifcallback(callback));
  }

  @Override
  public Builder upload(final Callback<Parser.ParamReference<Upload>> callback) {
    strategies.put(TypeLiteral.get(UploadParamReferenceImpl.class), callback);
    return this;
  }

  public Builder ifupload(final Callback<Parser.ParamReference<Upload>> callback) {
    return upload(ifcallback(callback));
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

  private <T> Callback<T> ifcallback(final Callback<T> callback) {
    return value -> {
      if (toType.getRawType() == Optional.class) {
        return ctx.next(toType, value);
      }
      return callback.invoke(value);
    };
  }
}

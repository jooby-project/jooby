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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Parser;
import org.jooby.Parser.BodyReference;
import org.jooby.Parser.Builder;
import org.jooby.Parser.Callback;
import org.jooby.Parser.ParamReference;
import org.jooby.Status;
import org.jooby.Upload;
import org.jooby.internal.StatusCodeProvider;
import org.jooby.internal.StrParamReferenceImpl;
import org.jooby.internal.UploadParamReferenceImpl;

import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

public class ParserExecutor {

  public static final Object NO_PARSER = new Object();

  private List<Parser> parsers;

  private Injector injector;

  private StatusCodeProvider sc;

  @Inject
  public ParserExecutor(final Injector injector, final Set<Parser> parsers,
      final StatusCodeProvider sc) {
    this.injector = injector;
    this.parsers = ImmutableList.copyOf(parsers);
    this.sc = sc;
  }

  public Status statusCode(final Throwable cause) {
    return sc.apply(cause);
  }

  public <T> T convert(final TypeLiteral<?> type, final Object data) throws Throwable {
    return convert(type, MediaType.plain, data);
  }

  @SuppressWarnings("unchecked")
  public <T> T convert(final TypeLiteral<?> type, final MediaType contentType, final Object data)
      throws Throwable {
    Object result = ctx(injector, contentType, type, parsers, data).next(type, data);
    return (T) result;
  }

  private static Parser.Context ctx(final Injector injector,
      final MediaType contentType, final TypeLiteral<?> seedType, final List<Parser> parsers,
      final Object seed) {
    return new Parser.Context() {
      int cursor = 0;

      TypeLiteral<?> type = seedType;

      ParserBuilder builder = new ParserBuilder(this, type, seed);

      @Override
      public MediaType type() {
        return contentType;
      }

      @Override
      public Builder body(final Callback<Parser.BodyReference> callback) {
        return builder.body(callback);
      }

      @Override
      public Builder ifbody(final Callback<BodyReference> callback) {
        return builder.ifbody(callback);
      }

      @Override
      public Builder upload(final Callback<Parser.ParamReference<Upload>> callback) {
        return builder.upload(callback);
      }

      @Override
      public Builder ifupload(final Callback<ParamReference<Upload>> callback) {
        return builder.ifupload(callback);
      }

      @Override
      public Builder param(final Callback<ParamReference<String>> callback) {
        return builder.param(callback);
      }

      @Override
      public Builder ifparam(final Callback<ParamReference<String>> callback) {
        return builder.ifparam(callback);
      }

      @Override
      public Builder params(final Callback<Map<String, Mutant>> callback) {
        return builder.params(callback);
      }

      @Override
      public Builder ifparams(final Callback<Map<String, Mutant>> callback) {
        return builder.ifparams(callback);
      }

      @Override
      public Object next() throws Throwable {
        return next(builder.toType, builder.value);
      }

      @Override
      public Object next(final TypeLiteral<?> type) throws Throwable {
        return next(type, builder.value);
      }

      @Override
      public Object next(final TypeLiteral<?> nexttype, final Object nextval)
          throws Throwable {
        if (cursor == parsers.size()) {
          return NO_PARSER;
        }
        if (!type.equals(nexttype)) {
          // reset cursor on type changes.
          cursor = 0;
          type = nexttype;
        }
        Parser next = parsers.get(cursor);
        cursor += 1;
        ParserBuilder current = builder;
        builder = new ParserBuilder(this, nexttype, wrap(nextval, builder.value));
        Object result = next.parse(nexttype, this);
        if (result instanceof ParserBuilder) {
          // call a parse
          result = ((ParserBuilder) result).parse();
        }
        builder = current;
        cursor -= 1;
        return result;
      }

      @SuppressWarnings("rawtypes")
      private Object wrap(final Object nextval, final Object value) {
        if (nextval instanceof String) {
          ParamReference<?> pref = (ParamReference) value;
          return new StrParamReferenceImpl(pref.type(), pref.name(),
              ImmutableList.of((String) nextval));
        } else if (nextval instanceof Upload) {
          ParamReference<?> pref = (ParamReference) value;
          return new UploadParamReferenceImpl(pref.name(), ImmutableList.of((Upload) nextval));
        }
        return nextval;
      }

      @Override
      public <T> T require(final Key<T> key) {
        return injector.getInstance(key);
      }

      @Override
      public <T> T require(final Class<T> type) {
        return injector.getInstance(type);
      }

      @Override
      public <T> T require(final TypeLiteral<T> type) {
        return injector.getInstance(Key.get(type));
      }

      @Override
      public String toString() {
        return parsers.toString();
      }
    };
  }

}

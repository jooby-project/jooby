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
package org.jooby.internal;

import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jooby.Parser;
import org.jooby.Upload;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.inject.TypeLiteral;

@SuppressWarnings({"unchecked", "rawtypes" })
public enum BuiltinParser implements Parser {

  Basic {
    private final Map<Class<?>, Function<String, Object>> parsers = ImmutableMap
        .<Class<?>, Function<String, Object>> builder()
        .put(BigDecimal.class, NOT_EMPTY.andThen(BigDecimal::new))
        .put(BigInteger.class, NOT_EMPTY.andThen(BigInteger::new))
        .put(Byte.class, NOT_EMPTY.andThen(Byte::valueOf))
        .put(byte.class, NOT_EMPTY.andThen(Byte::valueOf))
        .put(Double.class, NOT_EMPTY.andThen(Double::valueOf))
        .put(double.class, NOT_EMPTY.andThen(Double::valueOf))
        .put(Float.class, NOT_EMPTY.andThen(Float::valueOf))
        .put(float.class, NOT_EMPTY.andThen(Float::valueOf))
        .put(Integer.class, NOT_EMPTY.andThen(Integer::valueOf))
        .put(int.class, NOT_EMPTY.andThen(Integer::valueOf))
        .put(Long.class, NOT_EMPTY.andThen(this::toLong))
        .put(long.class, NOT_EMPTY.andThen(this::toLong))
        .put(Short.class, NOT_EMPTY.andThen(Short::valueOf))
        .put(short.class, NOT_EMPTY.andThen(Short::valueOf))
        .put(Boolean.class, NOT_EMPTY.andThen(this::toBoolean))
        .put(boolean.class, NOT_EMPTY.andThen(this::toBoolean))
        .put(Character.class, NOT_EMPTY.andThen(this::toCharacter))
        .put(char.class, NOT_EMPTY.andThen(this::toCharacter))
        .put(String.class, this::toString)
        .build();

    @Override
    public Object parse(final TypeLiteral<?> type, final Parser.Context ctx) throws Throwable {
      Function<String, Object> parser = parsers.get(type.getRawType());
      if (parser != null) {
        return ctx
            .param(values -> parser.apply(values.get(0))).body(body -> parser.apply(body.text()));
      }
      return ctx.next();
    }

    private String toString(final String value) {
      return value;
    }

    private char toCharacter(final String value) {
      return value.charAt(0);
    }

    private Boolean toBoolean(final String value) {
      if ("true".equals(value)) {
        return Boolean.TRUE;
      } else if ("false".equals(value)) {
        return Boolean.FALSE;
      }
      throw new IllegalArgumentException("Not a boolean: " + value);
    }

    private Long toLong(final String value) {
      try {
        return Long.valueOf(value);
      } catch (NumberFormatException ex) {
        // long as date, like If-Modified-Since
        try {
          LocalDateTime date = LocalDateTime.parse(value, Headers.fmt);
          Instant instant = date.toInstant(ZoneOffset.UTC);
          return instant.toEpochMilli();
        } catch (DateTimeParseException ignored) {
          throw ex;
        }
      }

    }
  },

  Collection {
    private final Map<Class<?>, Supplier<ImmutableCollection.Builder<?>>> parsers = ImmutableMap.<Class<?>, Supplier<ImmutableCollection.Builder<?>>> builder()
        .put(List.class, ImmutableList.Builder::new)
        .put(Set.class, ImmutableSet.Builder::new)
        .put(SortedSet.class, ImmutableSortedSet::naturalOrder)
        .build();

    private boolean matches(final TypeLiteral<?> toType) {
      return parsers.containsKey(toType.getRawType())
          && toType.getType() instanceof ParameterizedType;
    }

    @Override
    public Object parse(final TypeLiteral<?> type, final Parser.Context ctx) throws Throwable {
      if (matches(type)) {
        return ctx.param(values -> {
          ImmutableCollection.Builder builder = parsers.get(type.getRawType()).get();
          TypeLiteral<?> paramType = TypeLiteral.get(((ParameterizedType) type.getType())
              .getActualTypeArguments()[0]);
          for (Object value : values) {
            if (!paramType.getRawType().equals(String.class)) {
              for (Object csv : value.toString().split(",")) {
                builder.add(ctx.next(paramType, csv));
              }
            } else {
              builder.add(ctx.next(paramType, value));
            }
          }
          return builder.build();
        }).upload(uploads -> {
          ImmutableCollection.Builder builder = parsers.get(type.getRawType()).get();
          TypeLiteral<Upload> paramType = TypeLiteral.get(Upload.class);
          for (Upload upload : uploads) {
            builder.add(ctx.next(paramType, upload));
          }
          return builder.build();
        });
      } else {
        return ctx.next();
      }
    }
  },

  Optional {
    private boolean matches(final TypeLiteral<?> toType) {
      return Optional.class == toType.getRawType() && toType.getType() instanceof ParameterizedType;
    }

    @Override
    public Object parse(final TypeLiteral<?> type, final Parser.Context ctx)
        throws Throwable {
      if (matches(type)) {
        TypeLiteral<?> paramType = TypeLiteral.get(((ParameterizedType) type.getType())
            .getActualTypeArguments()[0]);
        return ctx
            .param(values -> {
              if (values.size() == 0) {
                return java.util.Optional.empty();
              }
              return java.util.Optional.of(ctx.next(paramType));
            }).body(body -> {
              if (body.length() == 0) {
                return java.util.Optional.empty();
              }
              return java.util.Optional.of(ctx.next(paramType));
            }).upload(files -> {
              return java.util.Optional.of(ctx.next(paramType));
            });
      } else {
        return ctx.next();
      }
    }
  },

  Enum {
    @Override
    public Object parse(final TypeLiteral<?> type, final Parser.Context ctx)
        throws Throwable {
      Class rawType = type.getRawType();
      if (Enum.class.isAssignableFrom(rawType)) {
        return ctx
            .param(values -> toEnum(rawType, values.get(0)))
            .body(body -> toEnum(rawType, body.text()));
      } else {
        return ctx.next();
      }
    }

    Object toEnum(final Class type, final String value) {
      Set<Enum> set = EnumSet.allOf(type);
      return set.stream()
          .filter(e -> e.name().equalsIgnoreCase(value))
          .findFirst()
          .orElseGet(() -> java.lang.Enum.valueOf(type, value));
    }
  },

  Upload {
    @Override
    public Object parse(final TypeLiteral<?> type, final Context ctx) throws Throwable {
      if (Upload.class == type.getRawType()) {
        return ctx.upload(uploads -> uploads.get(0));
      } else {
        return ctx.next();
      }
    }
  },

  Bytes {
    @Override
    public Object parse(final TypeLiteral<?> type, final Parser.Context ctx) throws Throwable {
      if (type.getRawType() == byte[].class) {
        return ctx.body(body -> body.bytes());
      }
      return ctx.next();
    }

    @Override
    public String toString() {
      return "byte[]";
    }
  }

}

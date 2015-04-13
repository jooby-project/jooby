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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.function.Function;

import org.jooby.ParamConverter;
import org.jooby.internal.Headers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.TypeLiteral;

public class CommonTypesParamConverter implements ParamConverter {

  private final Map<Class<?>, Function<String, Object>> parsers =
      ImmutableMap.<Class<?>, Function<String, Object>> builder()
          .put(BigDecimal.class, BigDecimal::new)
          .put(BigInteger.class, BigInteger::new)
          .put(Byte.class, Byte::valueOf)
          .put(byte.class, Byte::valueOf)
          .put(Double.class, Double::valueOf)
          .put(double.class, Double::valueOf)
          .put(Float.class, Float::valueOf)
          .put(float.class, Float::valueOf)
          .put(Integer.class, Integer::valueOf)
          .put(int.class, Integer::valueOf)
          .put(Long.class, CommonTypesParamConverter::toLong)
          .put(long.class, CommonTypesParamConverter::toLong)
          .put(Short.class, Short::valueOf)
          .put(short.class, Short::valueOf)
          .put(Boolean.class, CommonTypesParamConverter::toBoolean)
          .put(boolean.class, CommonTypesParamConverter::toBoolean)
          .put(Character.class, CommonTypesParamConverter::toCharacter)
          .put(char.class, CommonTypesParamConverter::toCharacter)
          .put(String.class, CommonTypesParamConverter::toString)
          .build();

  @Override
  public Object convert(final TypeLiteral<?> toType, final Object[] values, final Context ctx)
      throws Exception {
    Function<String, Object> parser = parsers.get(toType.getRawType());
    if (parser == null) {
      return ctx.convert(toType, values);
    }
    return parser.apply((String) values[0]);
  }

  private static String toString(final String value) {
    return value;
  }

  private static char toCharacter(final String value) {
    return value.charAt(0);
  }

  private static Boolean toBoolean(final String value) {
    if ("true".equals(value)) {
      return Boolean.TRUE;
    } else if ("false".equals(value)) {
      return Boolean.FALSE;
    }
    throw new IllegalArgumentException("Not a boolean: " + value);
  }

  private static Long toLong(final String value) {
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

}

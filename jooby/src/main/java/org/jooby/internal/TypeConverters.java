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

import java.util.Locale;

import org.jooby.internal.reqparam.LocaleParamConverter;
import org.jooby.internal.reqparam.StaticMethodParamConverter;
import org.jooby.internal.reqparam.StringConstructorParamConverter;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.TypeConverter;

public class TypeConverters {

  public static void configure(final Binder binder) {
    binder.convertToTypes(staticMethodMatcher("valueOf"),
        staticMethodTypeConverter("valueOf"));

    binder.convertToTypes(staticMethodMatcher("fromString"),
        staticMethodTypeConverter("fromString"));

    binder.convertToTypes(staticMethodMatcher("forName"),
        staticMethodTypeConverter("forName"));

    binder.convertToTypes(stringConstructorMatcher(), stringConstructorTypeConverter());
  }

  private static TypeConverter stringConstructorTypeConverter() {
    return (value, type) -> {
      Class<?> rawType = type.getRawType();
      try {
        if (rawType == Locale.class) {
          return new LocaleParamConverter().convert(type, new Object[]{value }, null);
        }
        return new StringConstructorParamConverter().convert(type, new Object[]{value }, null);
      } catch (Exception ex) {
        throw new IllegalStateException("Can't convert: " + value + " to " + type, ex);
      }
    };
  }

  private static Matcher<TypeLiteral<?>> stringConstructorMatcher() {
    return new AbstractMatcher<TypeLiteral<?>>() {
      @Override
      public boolean matches(final TypeLiteral<?> type) {
        return new StringConstructorParamConverter().matches(type);
      }

      @Override
      public String toString() {
        return "TypeConverter init(String)";
      }
    };
  }

  private static TypeConverter staticMethodTypeConverter(final String name) {
    return new TypeConverter() {
      @Override
      public Object convert(final String value, final TypeLiteral<?> toType) {
        try {
          return new StaticMethodParamConverter(name).convert(toType, new Object[]{value }, null);
        } catch (Exception ex) {
          throw new IllegalStateException("Can't convert: " + value + " to " + toType, ex);
        }
      }

      @Override
      public String toString() {
        return name + "(String)";
      }
    };
  }

  private static Matcher<TypeLiteral<?>> staticMethodMatcher(final String name) {
    return new AbstractMatcher<TypeLiteral<?>>() {
      @Override
      public boolean matches(final TypeLiteral<?> type) {
        return !Enum.class.isAssignableFrom(type.getRawType())
            && new StaticMethodParamConverter(name).matches(type);
      }

      @Override
      public String toString() {
        return name + "(String)";
      }
    };
  }

}

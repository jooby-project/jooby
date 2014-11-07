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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.TypeConverter;

public class TypeConverters {

  public static void configure(final Binder binder) {
    binder.convertToTypes(stringConstructorMatcher(), stringConstructorTypeConverter());

    binder.convertToTypes(staticMethodMatcher("valueOf"),
        staticMethodTypeConverter("valueOf"));

    binder.convertToTypes(staticMethodMatcher("fromString"),
        staticMethodTypeConverter("fromString"));

    binder.convertToTypes(staticMethodMatcher("forName"),
        staticMethodTypeConverter("forName"));

    binder.convertToTypes(localeMatcher(), localeTypeConverter());

  }

  private static TypeConverter localeTypeConverter() {
    return new TypeConverter() {

      @Override
      public Object convert(final String value, final TypeLiteral<?> type) {
        String[] locale = value.split("_");
        return locale.length == 1 ? new Locale(locale[0]) : new Locale(locale[0], locale[1]);
      }
    };
  }

  private static Matcher<TypeLiteral<?>> localeMatcher() {
    return new AbstractMatcher<TypeLiteral<?>>() {
      @Override
      public boolean matches(final TypeLiteral<?> type) {
        Class<?> rawType = type.getRawType();
        return rawType == Locale.class;
      }

      @Override
      public String toString() {
        return "Locale(String, String)";
      }
    };
  }

  private static TypeConverter stringConstructorTypeConverter() {
    return new TypeConverter() {

      @Override
      public Object convert(final String value, final TypeLiteral<?> type) {
        Class<?> rawType = type.getRawType();
        try {
          return rawType.getDeclaredConstructor(String.class).newInstance(value);
        } catch (NoSuchMethodException | SecurityException | InstantiationException
            | InvocationTargetException | IllegalAccessException ex) {
          throw new IllegalStateException("Invocation of " + rawType.getName() + "(String) failed",
              ex);
        }
      }
    };
  }

  private static Matcher<TypeLiteral<?>> stringConstructorMatcher() {
    return new AbstractMatcher<TypeLiteral<?>>() {
      @Override
      public boolean matches(final TypeLiteral<?> type) {
        Class<?> rawType = type.getRawType();
        try {
          rawType.getDeclaredConstructor(String.class);
          return true;
        } catch (NoSuchMethodException | SecurityException ex) {
          return false;
        }
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
        Class<?> rawType = toType.getRawType();
        try {
          Method method = method(rawType, name);
          return method.invoke(null, value);
        } catch (ReflectiveOperationException ex) {
          throw new IllegalArgumentException("Execution of: " + rawType + "." + name
              + " results in error", ex);
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
        Method method = method(type.getRawType(), name);
        if (method == null) {
          return false;
        }
        int mods = method.getModifiers();
        return Modifier.isStatic(mods) && Modifier.isPublic(mods);
      }

      @Override
      public String toString() {
        return name + "(String)";
      }
    };
  }

  private static Method method(final Class<?> type, final String name) {
    try {
      return type.getDeclaredMethod(name, String.class);
    } catch (NoSuchMethodException | SecurityException | IllegalArgumentException ex) {
      return null;
    }
  }

}

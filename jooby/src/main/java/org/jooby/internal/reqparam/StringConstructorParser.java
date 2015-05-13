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

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.jooby.Parser;

import com.google.inject.TypeLiteral;

public class StringConstructorParser implements Parser {

  public boolean matches(final TypeLiteral<?> toType) {
    return constructor(toType.getRawType()) != null;
  }

  @Override
  public Object parse(final TypeLiteral<?> type, final Parser.Context ctx) throws Exception {
    return ctx.param(params -> {
      Constructor<?> constructor = constructor(type.getRawType());
      if (constructor == null) {
        return ctx.next();
      }
      return constructor.newInstance(params.get(0));
    });
  }

  @Override
  public String toString() {
    return "init(String)";
  }

  public static Object parse(final TypeLiteral<?> type, final Object data) throws Exception {
    return constructor(type.getRawType()).newInstance(data);
  }

  private static Constructor<?> constructor(final Class<?> rawType) {
    try {
      Constructor<?> constructor = rawType.getDeclaredConstructor(String.class);
      return Modifier.isPublic(constructor.getModifiers()) ? constructor : null;
    } catch (NoSuchMethodException | SecurityException ex) {
      return null;
    }
  }

}

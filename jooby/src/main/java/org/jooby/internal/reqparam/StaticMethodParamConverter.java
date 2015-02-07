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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.jooby.ParamConverter;

import com.google.inject.TypeLiteral;

public class StaticMethodParamConverter implements ParamConverter {

  private String methodName;

  public StaticMethodParamConverter(final String methodName) {
    this.methodName = requireNonNull(methodName, "A method's name is required.");
  }

  public boolean matches(final TypeLiteral<?> toType) {
    return method(toType.getRawType()) != null;
  }

  @Override
  public Object convert(final TypeLiteral<?> toType, final Object[] values, final Context ctx)
      throws Exception {
    Method method = method(toType.getRawType());
    if (method == null) {
      return ctx.convert(toType, values);
    }
    return method(toType.getRawType()).invoke(null, values[0]);
  }

  private Method method(final Class<?> rawType) {
    try {
      Method method = rawType.getDeclaredMethod(methodName, String.class);
      int mods = method.getModifiers();
      return Modifier.isPublic(mods) && Modifier.isStatic(mods) ? method : null;
    } catch (NoSuchMethodException | SecurityException ex) {
      return null;
    }
  }

}

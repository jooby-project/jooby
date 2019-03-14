/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.annotations;

import io.jooby.Context;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

interface ContextValue {
  Object apply(Context ctx);

  static ContextValue context() {
    return ctx -> ctx;
  }

  static ContextValue create(Parameter parameter) {
    Type type = parameter.getParameterizedType();
    Class<?> rawType = parameter.getType();
    if (rawType == Context.class) {
      return context();
    }
    /**
     * Path:
     */
    PathParam pathParam = parameter.getAnnotation(PathParam.class);
    if (pathParam != null) {
      String key = pathParam.value();
      if (key.length() == 0) {
        key = parameter.getName();
      }
      return pathParam(type, key);
    }
    throw new IllegalArgumentException(
        "Unable to provision `" + parameter +"` on route: " + parameter
            .getDeclaringExecutable());
  }

  static ContextValue pathParam(Type type, String key) {
    return ctx -> ctx.path(key).to(type);
  }
}

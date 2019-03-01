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

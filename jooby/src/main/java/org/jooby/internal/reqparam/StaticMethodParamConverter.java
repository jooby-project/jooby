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
  public Object convert(final TypeLiteral<?> toType, final Object[] values, final Chain chain)
      throws Exception {
    Method method = method(toType.getRawType());
    if (method == null) {
      return chain.convert(toType, values);
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

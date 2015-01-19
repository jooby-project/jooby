package org.jooby.internal.reqparam;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.jooby.ParamConverter;

import com.google.inject.TypeLiteral;

public class StringConstructorParamConverter implements ParamConverter {

  public boolean matches(final TypeLiteral<?> toType) {
    return constructor(toType.getRawType()) != null;
  }

  @Override
  public Object convert(final TypeLiteral<?> toType, final Object[] values, final Chain chain)
      throws Exception {
    Constructor<?> constructor = constructor(toType.getRawType());
    if (constructor == null) {
      return chain.convert(toType, values);
    }
    return constructor(toType.getRawType()).newInstance(values[0]);
  }

  private Constructor<?> constructor(final Class<?> rawType) {
    try {
      Constructor<?> constructor = rawType.getDeclaredConstructor(String.class);
      return Modifier.isPublic(constructor.getModifiers()) ? constructor : null;
    } catch (NoSuchMethodException | SecurityException ex) {
      return null;
    }
  }

}

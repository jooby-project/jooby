package org.jooby.internal.reqparam;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

import org.jooby.ParamConverter;

import com.google.inject.TypeLiteral;

public class OptionalParamConverter implements ParamConverter {

  private boolean matches(final TypeLiteral<?> toType) {
    return Optional.class == toType.getRawType() && toType.getType() instanceof ParameterizedType;
  }

  @Override
  public Object convert(final TypeLiteral<?> toType, final Object[] values, final Chain chain)
      throws Exception {
    if (matches(toType)) {
      if (values == null) {
        return Optional.empty();
      }
      TypeLiteral<?> paramType = TypeLiteral.get(((ParameterizedType) toType.getType())
          .getActualTypeArguments()[0]);
      return Optional.of(chain.convert(paramType, values));
    } else {
      return chain.convert(toType, values);
    }
  }

}

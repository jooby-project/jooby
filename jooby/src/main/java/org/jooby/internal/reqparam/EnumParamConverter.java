package org.jooby.internal.reqparam;

import org.jooby.ParamConverter;

import com.google.inject.TypeLiteral;

public class EnumParamConverter implements ParamConverter {

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  public Object convert(final TypeLiteral<?> toType, final Object[] values, final Chain chain)
      throws Exception {
    Class rawType = toType.getRawType();
    if (Enum.class.isAssignableFrom(rawType)) {
      return Enum.valueOf(rawType, (String) values[0]);
    } else {
      return chain.convert(toType, values);
    }
  }

}

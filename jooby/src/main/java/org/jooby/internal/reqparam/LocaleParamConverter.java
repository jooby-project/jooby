package org.jooby.internal.reqparam;

import java.util.Locale;

import org.jooby.ParamConverter;
import org.jooby.internal.LocaleUtils;

import com.google.inject.TypeLiteral;

public class LocaleParamConverter implements ParamConverter {

  @Override
  public Object convert(final TypeLiteral<?> toType, final Object[] values, final Chain chain)
      throws Exception {
    if (Locale.class == toType.getRawType()) {
      return LocaleUtils.toLocale((String) values[0]);
    } else {
      return chain.convert(toType, values);
    }
  }

}

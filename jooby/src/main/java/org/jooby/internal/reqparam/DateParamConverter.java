package org.jooby.internal.reqparam;

import static java.util.Objects.requireNonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jooby.ParamConverter;

import com.google.inject.TypeLiteral;

public class DateParamConverter implements ParamConverter {

  private String dateFormat;

  public DateParamConverter(final String dateFormat) {
    this.dateFormat = requireNonNull(dateFormat, "A dateFormat is required.");
  }

  @Override
  public Object convert(final TypeLiteral<?> toType, final Object[] values, final Chain chain)
      throws Exception {
    if (toType.getRawType() == Date.class) {
      try {
        return new Date(Long.parseLong((String) values[0]));
      } catch (NumberFormatException ex) {
        return new SimpleDateFormat(dateFormat).parse((String) values[0]);
      }
    } else {
      return chain.convert(toType, values);
    }
  }

}

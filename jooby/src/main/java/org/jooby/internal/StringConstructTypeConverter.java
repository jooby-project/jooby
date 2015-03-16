package org.jooby.internal;

import java.util.Locale;

import org.jooby.internal.reqparam.LocaleParamConverter;
import org.jooby.internal.reqparam.StringConstructorParamConverter;

import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.TypeConverter;

class StringConstructTypeConverter<T> extends AbstractMatcher<TypeLiteral<T>>
    implements TypeConverter {

  @Override
  public Object convert(final String value, final TypeLiteral<?> type) {
    Class<?> rawType = type.getRawType();
    try {
      if (rawType == Locale.class) {
        return new LocaleParamConverter().convert(type, new Object[]{value }, null);
      }
      return new StringConstructorParamConverter().convert(type, new Object[]{value }, null);
    } catch (Exception ex) {
      throw new IllegalStateException("Can't convert: " + value + " to " + type, ex);
    }
  }

  @Override
  public boolean matches(final TypeLiteral<T> type) {
    return new StringConstructorParamConverter().matches(type);
  }

  @Override
  public String toString() {
    return "TypeConverter init(java.lang.String)";
  }

}

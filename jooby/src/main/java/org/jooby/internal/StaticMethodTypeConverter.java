package org.jooby.internal;

import org.jooby.internal.reqparam.StaticMethodParamConverter;

import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.spi.TypeConverter;

class StaticMethodTypeConverter<T> extends AbstractMatcher<TypeLiteral<T>>
    implements TypeConverter {

  private StaticMethodParamConverter converter;

  public StaticMethodTypeConverter(final String name) {
    converter = new StaticMethodParamConverter(name);
  }

  @Override
  public Object convert(final String value, final TypeLiteral<?> type) {
    try {
      return converter.convert(type, new Object[]{value }, null);
    } catch (Exception ex) {
      throw new IllegalStateException("Can't convert: " + value + " to " + type, ex);
    }
  }

  @Override
  public boolean matches(final TypeLiteral<T> type) {
    return !Enum.class.isAssignableFrom(type.getRawType())
        && converter.matches(type);
  }

  @Override
  public String toString() {
    return converter.toString();
  }

}

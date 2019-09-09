package io.jooby.internal.converter;

import io.jooby.spi.ValueConverter;

import java.math.BigInteger;

public class BigIntegerConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == BigInteger.class;
  }

  @Override public BigInteger convert(Class type, String value) {
    return new BigInteger(value);
  }
}

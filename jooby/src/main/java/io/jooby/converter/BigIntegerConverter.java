package io.jooby.converter;

import java.math.BigInteger;

public class BigIntegerConverter implements ValueConverter {
  @Override public boolean supports(Class type) {
    return type == BigInteger.class;
  }

  @Override public BigInteger convert(Class type, String value) {
    return new BigInteger(value);
  }
}

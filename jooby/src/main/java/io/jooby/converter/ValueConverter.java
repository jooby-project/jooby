package io.jooby.converter;

public interface ValueConverter {
  boolean supports(Class type);

  Object convert(Class type, String value);
}

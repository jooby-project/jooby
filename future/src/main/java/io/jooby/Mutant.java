package io.jooby;

import java.util.List;

public interface Mutant {

  default int intValue() {
    return Integer.parseInt(value());
  }

  default String value() {
    List<String> values = values();
    return values.get(0);
  }

  List<String> values();

  default <T> T to(Class<? extends T> type) {
    if (type == String.class) {
      return (T) value();
    }
    if (type == Integer.TYPE || type == Integer.class) {
      return (T) ((Integer) intValue());
    }
    throw new IllegalArgumentException("No parameter converter for " + type);
  }
}

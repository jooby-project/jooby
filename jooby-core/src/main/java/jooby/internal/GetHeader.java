package jooby.internal;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeConverterBinding;

public class GetHeader extends VariantImpl {

  private static final Set<String> DATES = ImmutableSet.of("if-modified-since",
      "if-range", "if-unmodified-since");

  public GetHeader(final String name, final List<String> values,
      final Set<TypeConverterBinding> typeConverters) {
    super(name, values, typeConverters);
  }

  @Override
  public long longValue() {
    if (DATES.contains(name.toLowerCase())) {
      // it is date!
      return asDateInMillis();
    }
    try {
      // assume it is a valid long
      return super.longValue();
    } catch (RuntimeException ex) {
      try {
        return asDateInMillis();
      } catch (RuntimeException ignored) {
        throw ex;
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T to(final TypeLiteral<T> type) {
    Class<? super T> rawType = type.getRawType();
    if (rawType == long.class || rawType == Long.class) {
      Long value = longValue();
      return (T) value;
    }
    return super.to(type);
  }

  private long asDateInMillis() {
    if (values == null || values.size() == 0) {
      return -1;
    }
    DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    LocalDateTime date = LocalDateTime.parse(values.get(0), formatter);
    Instant instant = date.toInstant(ZoneOffset.UTC);
    return instant.toEpochMilli();
  }
}

/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.value;

import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;

/** Collection of built-in/standard value converters. */
public enum StandardConverter implements Converter {
  /** Convert a value to string. */
  String {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(java.lang.String.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      return value.valueOrNull();
    }
  },
  /** Convert a value to int/Integer. */
  Int {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(int.class, this);
      factory.put(Integer.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      if (type == int.class) {
        return value.intValue();
      }
      return value.isMissing() ? null : value.intValue();
    }
  },
  /** Convert a value to long/Long. */
  Long {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(long.class, this);
      factory.put(Long.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      if (type == long.class) {
        return value.longValue();
      }
      return value.isMissing() ? null : value.longValue();
    }
  },
  /** Convert a value to float/Float. */
  Float {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(float.class, this);
      factory.put(Float.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      if (type == float.class) {
        return value.floatValue();
      }
      return value.isMissing() ? null : value.floatValue();
    }
  },
  /** Convert a value to double/Double. */
  Double {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(double.class, this);
      factory.put(Double.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      if (type == double.class) {
        return value.doubleValue();
      }
      return value.isMissing() ? null : value.doubleValue();
    }
  },
  /** Convert a value to boolean/Boolean. */
  Boolean {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(boolean.class, this);
      factory.put(Boolean.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      if (type == boolean.class) {
        return value.booleanValue();
      }
      return value.isMissing() ? null : value.booleanValue();
    }
  },
  /** Convert a value to byte/Byte. */
  Byte {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(byte.class, this);
      factory.put(Byte.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      if (type == byte.class) {
        return value.byteValue();
      }
      return value.isMissing() ? null : value.byteValue();
    }
  },
  /** Convert a value to BigDecimal. */
  BigDecimal {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(BigDecimal.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      return new BigDecimal(value.value());
    }
  },
  /** Convert a value to BigInteger. */
  BigInteger {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(BigInteger.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      return new BigInteger(value.value());
    }
  },
  /** Convert a value to Charset. */
  Charset {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(Charset.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      var charset = value.value();
      return switch (charset.toLowerCase()) {
        case "utf-8" -> StandardCharsets.UTF_8;
        case "us-ascii" -> StandardCharsets.US_ASCII;
        case "iso-8859-1" -> StandardCharsets.ISO_8859_1;
        case "utf-16" -> StandardCharsets.UTF_16;
        case "utf-16be" -> StandardCharsets.UTF_16BE;
        case "utf-16le" -> StandardCharsets.UTF_16LE;
        default -> java.nio.charset.Charset.forName(charset);
      };
    }
  },
  /** Convert a value to Date. */
  Date {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(Date.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      try {
        // must be millis
        return new Date(parseLong(value.value()));
      } catch (NumberFormatException x) {
        // must be YYYY-MM-dd
        var date = java.time.LocalDate.parse(value.value(), DateTimeFormatter.ISO_LOCAL_DATE);
        return java.util.Date.from(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
      }
    }
  },
  /** Convert a value to Duration. */
  Duration {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(Duration.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      try {
        return java.time.Duration.parse(value.value());
      } catch (DateTimeParseException x) {
        var millis = MILLISECONDS.convert(parseDuration(value.value()), NANOSECONDS);
        return java.time.Duration.ofMillis(millis);
      }
    }

    /**
     * Parses a duration string. If no units are specified in the string, it is assumed to be in
     * milliseconds. The returned duration is in nanoseconds. The purpose of this function is to
     * implement the duration-related methods in the ConfigObject interface.
     *
     * @param value the string to parse
     * @return duration in nanoseconds
     */
    private static long parseDuration(String value) {
      var unitString = getUnits(value);
      var numberString = value.substring(0, value.length() - unitString.length());

      // this would be caught later anyway, but the error message
      // is more helpful if we check it here.
      if (numberString.isEmpty()) {
        throw new DateTimeParseException(
            "No number in duration value: '" + numberString + "'", value, 0);
      }

      // note that this is deliberately case-sensitive
      var units =
          switch (unitString) {
            case "ms", "milli", "millis", "millisecond", "milliseconds", "" -> MILLISECONDS;
            case "us", "micro", "micros", "microsecond", "microseconds" -> TimeUnit.MICROSECONDS;
            case "ns", "nano", "nanos", "nanosecond", "nanoseconds" -> NANOSECONDS;
            case "s", "second", "seconds" -> TimeUnit.SECONDS;
            case "m", "minute", "minutes" -> TimeUnit.MINUTES;
            case "h", "hour", "hours" -> TimeUnit.HOURS;
            case "d", "day", "days" -> TimeUnit.DAYS;
            default ->
                throw new DateTimeParseException(
                    "Could not parse time unit '" + unitString + "'",
                    value,
                    value.length() - unitString.length());
          };
      try {
        // if the string is purely digits, parse as an integer to avoid possible precision loss;
        // otherwise as a double.
        if (numberString.matches("[+-]?[0-9]+")) {
          return units.toNanos(parseLong(numberString));
        } else {
          long nanosInUnit = units.toNanos(1);
          return (long) (parseDouble(numberString) * nanosInUnit);
        }
      } catch (NumberFormatException e) {
        throw new DateTimeParseException(
            "Could not parse duration number '" + numberString + "'", numberString, 0);
      }
    }
  },
  /** Convert a value to Period. */
  Period {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(Period.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      try {
        return java.time.Period.from((Duration) Duration.convert(type, value, hint));
      } catch (DateTimeException x) {
        return parsePeriod(value.value());
      }
    }

    /**
     * Parses a period string. If no units are specified in the string, it is assumed to be in days.
     * The returned period is in days. The purpose of this function is to implement the
     * period-related methods in the ConfigObject interface.
     *
     * @param value the string to parse path to include in exceptions
     * @return duration in days
     */
    public static Period parsePeriod(String value) {
      var unitString = getUnits(value);
      var numberString = value.substring(0, value.length() - unitString.length());

      // this would be caught later anyway, but the error message
      // is more helpful if we check it here.
      if (numberString.isEmpty())
        throw new DateTimeParseException(
            "No number in period value '" + numberString + "'", numberString, 0);

      var units =
          switch (unitString) {
            case "d", "day", "days", "" -> ChronoUnit.DAYS;
            case "w", "week", "weeks" -> ChronoUnit.WEEKS;
            case "m", "mo", "month", "months" -> ChronoUnit.MONTHS;
            case "y", "year", "years" -> ChronoUnit.YEARS;
            default ->
                throw new DateTimeParseException(
                    "Could not parse time unit '" + unitString + "' (try d, w, mo, y)",
                    value,
                    value.length() - unitString.length());
          };
      try {
        return periodOf(Integer.parseInt(numberString), units);
      } catch (NumberFormatException e) {
        throw new DateTimeParseException(
            "Could not parse duration number '" + numberString + "'", numberString, 0);
      }
    }

    private static Period periodOf(int n, ChronoUnit unit) {
      if (unit.isTimeBased()) {
        throw new DateTimeException(unit + " cannot be converted to a java.time.Period");
      }

      return switch (unit) {
        case DAYS -> java.time.Period.ofDays(n);
        case WEEKS -> java.time.Period.ofWeeks(n);
        case MONTHS -> java.time.Period.ofMonths(n);
        case YEARS -> java.time.Period.ofYears(n);
        default -> throw new DateTimeException(unit + " cannot be converted to a java.time.Period");
      };
    }
  },
  /** Convert a value to Instant. */
  Instant {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(Instant.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      try {
        return java.time.Instant.ofEpochMilli(parseLong(value.value()));
      } catch (NumberFormatException x) {
        return DateTimeFormatter.ISO_INSTANT.parse(value.value(), java.time.Instant::from);
      }
    }
  },
  /** Convert a value to LocalDate. */
  LocalDate {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(LocalDate.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      try {
        // must be millis
        var instant = java.time.Instant.ofEpochMilli(parseLong(value.value()));
        return instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
      } catch (NumberFormatException x) {
        // must be YYYY-MM-dd
        return java.time.LocalDate.parse(value.value(), DateTimeFormatter.ISO_LOCAL_DATE);
      }
    }
  },
  /** Convert a value to LocalDateTime. */
  LocalDateTime {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(LocalDateTime.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      try {
        // must be millis
        var instant = java.time.Instant.ofEpochMilli(parseLong(value.value()));
        return instant.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
      } catch (NumberFormatException x) {
        // must be YYYY-MM-dd
        return java.time.LocalDateTime.parse(value.value(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      }
    }
  },
  /** Convert a value to StatusCode. */
  StatusCode {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(StatusCode.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      return io.jooby.StatusCode.valueOf(value.intValue());
    }
  },
  /** Convert a value to TimeZone. */
  TimeZone {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(TimeZone.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      return java.util.TimeZone.getTimeZone(value.value());
    }
  },
  /** Convert a value to URI. */
  URI {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(URI.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      try {
        var uri = java.net.URI.create(value.value());
        if (type == URL.class) {
          return uri.toURL();
        }
        return uri;
      } catch (MalformedURLException x) {
        throw SneakyThrows.propagate(x);
      }
    }
  },
  /** Convert a value to UUID. */
  UUID {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(UUID.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      return java.util.UUID.fromString(value.value());
    }
  },
  /** Convert a value to ZoneId. */
  ZoneId {
    @Override
    protected void add(ValueFactory factory) {
      factory.put(ZoneId.class, this);
    }

    @Override
    public Object convert(@NonNull Type type, @NonNull Value value, @NonNull ConversionHint hint) {
      var zoneId = value.value();
      return java.time.ZoneId.of(java.time.ZoneId.SHORT_IDS.getOrDefault(zoneId, zoneId));
    }
  };

  private static String getUnits(String s) {
    int i = s.length() - 1;
    while (i >= 0) {
      char c = s.charAt(i);
      if (!Character.isLetter(c)) break;
      i -= 1;
    }
    return s.substring(i + 1);
  }

  protected abstract void add(ValueFactory factory);

  /**
   * Add all the standard converter to a ValueFactory.
   *
   * @param factory Source value factory.
   */
  public static void register(ValueFactory factory) {
    for (var converter : values()) {
      converter.add(factory);
    }
  }
}

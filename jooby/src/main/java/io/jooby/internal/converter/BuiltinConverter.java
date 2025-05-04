/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

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
import io.jooby.Value;
import io.jooby.ValueConverter;

public enum BuiltinConverter implements ValueConverter<Value> {
  BigDecimal {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == BigDecimal.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
      return new BigDecimal(value.value());
    }
  },
  BigInteger {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == BigInteger.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
      return new BigInteger(value.value());
    }
  },
  Charset {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == Charset.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
      String charset = value.value();
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
  Date {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == Date.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
      try {
        // must be millis
        return new Date(Long.parseLong(value.value()));
      } catch (NumberFormatException x) {
        // must be YYYY-MM-dd
        var date = java.time.LocalDate.parse(value.value(), DateTimeFormatter.ISO_LOCAL_DATE);
        return java.util.Date.from(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
      }
    }
  },
  Duration {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == Duration.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
      try {
        return java.time.Duration.parse(value.value());
      } catch (DateTimeParseException x) {
        var nanos = MILLISECONDS.convert(parseDuration(value.value()), NANOSECONDS);
        return java.time.Duration.ofMillis(nanos);
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
          return units.toNanos(Long.parseLong(numberString));
        } else {
          long nanosInUnit = units.toNanos(1);
          return (long) (Double.parseDouble(numberString) * nanosInUnit);
        }
      } catch (NumberFormatException e) {
        throw new DateTimeParseException(
            "Could not parse duration number '" + numberString + "'", numberString, 0);
      }
    }
  },
  Period {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == Period.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
      try {
        return java.time.Period.from((Duration) Duration.convert(value, type));
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
  Instant {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == Instant.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
      try {
        return java.time.Instant.ofEpochMilli(Long.parseLong(value.value()));
      } catch (NumberFormatException x) {
        return DateTimeFormatter.ISO_INSTANT.parse(value.value(), java.time.Instant::from);
      }
    }
  },
  LocalDate {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == LocalDate.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
      try {
        // must be millis
        var instant = java.time.Instant.ofEpochMilli(Long.parseLong(value.value()));
        return instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
      } catch (NumberFormatException x) {
        // must be YYYY-MM-dd
        return java.time.LocalDate.parse(value.value(), DateTimeFormatter.ISO_LOCAL_DATE);
      }
    }
  },
  LocalDateTime {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == LocalDateTime.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
      try {
        // must be millis
        var instant = java.time.Instant.ofEpochMilli(Long.parseLong(value.value()));
        return instant.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
      } catch (NumberFormatException x) {
        // must be YYYY-MM-dd
        return java.time.LocalDateTime.parse(value.value(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      }
    }
  },
  StatusCode {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == StatusCode.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
      return io.jooby.StatusCode.valueOf(value.intValue());
    }
  },
  TimeZone {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == TimeZone.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
      return java.util.TimeZone.getTimeZone(value.value());
    }
  },
  URI {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == URI.class || type == URL.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
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
  UUID {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == UUID.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
      return java.util.UUID.fromString(value.value());
    }
  },
  ZoneId {
    @Override
    public boolean supports(@NonNull Class<?> type) {
      return type == ZoneId.class;
    }

    @Override
    public @NonNull Object convert(@NonNull Value value, @NonNull Class<?> type) {
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
}

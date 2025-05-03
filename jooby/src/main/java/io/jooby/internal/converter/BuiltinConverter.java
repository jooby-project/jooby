/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.converter;

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
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
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
        try {
          long duration =
              ConfigFactory.empty()
                  .withValue("d", ConfigValueFactory.fromAnyRef(value.value()))
                  .getDuration("d", TimeUnit.MILLISECONDS);
          return java.time.Duration.ofMillis(duration);
        } catch (ConfigException ignored) {
          throw x;
        }
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
      return java.time.Period.from((Duration) Duration.convert(value, type));
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
}

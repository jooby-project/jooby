package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Headers {

  public static final DateTimeFormatter fmt = DateTimeFormatter
      .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
      .withZone(ZoneId.of("GMT"));

  public static String encode(final Object value) {
    requireNonNull(value, "Header value is required.");
    if (value instanceof Date) {
      return fmt.format(Instant.ofEpochMilli(((Date) value).getTime()));
    }
    if (value instanceof Calendar) {
      return fmt.format(Instant.ofEpochMilli(((Calendar) value).getTimeInMillis()));
    }
    return value.toString();
  }

}

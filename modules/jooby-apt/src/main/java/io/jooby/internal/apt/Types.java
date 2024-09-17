/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.apt;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.util.Set;
import java.util.TimeZone;

class Types {
  static final Set<String> BUILT_IN =
      Set.of(
          String.class.getName(),
          Boolean.class.getName(),
          Boolean.TYPE.getName(),
          Byte.class.getName(),
          Byte.TYPE.getName(),
          Character.class.getName(),
          Character.TYPE.getName(),
          Short.class.getName(),
          Short.TYPE.getName(),
          Integer.class.getName(),
          Integer.TYPE.getName(),
          Long.class.getName(),
          Long.TYPE.getName(),
          Float.class.getName(),
          Float.TYPE.getName(),
          Double.class.getName(),
          Double.TYPE.getName(),
          Enum.class.getName(),
          java.util.UUID.class.getName(),
          java.time.Instant.class.getName(),
          java.util.Date.class.getName(),
          java.time.LocalDate.class.getName(),
          java.time.LocalDateTime.class.getName(),
          java.math.BigDecimal.class.getName(),
          java.math.BigInteger.class.getName(),
          Duration.class.getName(),
          Period.class.getName(),
          java.nio.charset.Charset.class.getName(),
          "io.jooby.StatusCode",
          TimeZone.class.getName(),
          ZoneId.class.getName(),
          URI.class.getName(),
          URL.class.getName());
}

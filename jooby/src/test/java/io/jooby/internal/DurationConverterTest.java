/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.Period;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.jooby.Value;
import io.jooby.internal.converter.BuiltinConverter;

public class DurationConverterTest {

  @Test
  public void convertDuration() {
    var nanos = System.nanoTime();
    assertEquals(Duration.ofSeconds(1), duration("1s"));
    assertEquals(Duration.ofNanos(nanos).toMillis(), duration(nanos + "ns").toMillis());
    assertEquals(TimeUnit.MICROSECONDS.toMillis(1000), duration((1000) + "us").toMillis());
    assertEquals(Duration.ofMillis(500), duration("500ms"));
    assertEquals(Duration.ofMinutes(5), duration("5m"));
    assertEquals(Duration.ofDays(8), duration("8d"));
    assertEquals(Duration.ofHours(15), duration("15h"));
  }

  @Test
  public void convertPeriod() {
    assertEquals(Period.ofDays(1), period("1d"));
    assertEquals(Period.ofDays(1), period("1"));
    assertEquals(Period.ofDays(1), period("1day"));
    assertEquals(Period.ofDays(1), period("1days"));

    assertEquals(Period.ofMonths(2), period("2m"));
    assertEquals(Period.ofMonths(3), period("3mo"));
    assertEquals(Period.ofMonths(4), period("4month"));
    assertEquals(Period.ofMonths(5), period("5months"));

    assertEquals(Period.ofWeeks(2), period("2w"));
    assertEquals(Period.ofWeeks(3), period("3week"));
    assertEquals(Period.ofWeeks(4), period("4weeks"));

    assertEquals(Period.ofYears(2), period("2y"));
    assertEquals(Period.ofYears(3), period("3year"));
    assertEquals(Period.ofYears(4), period("4years"));
  }

  private Duration duration(String value) {
    return (Duration) BuiltinConverter.Duration.convert(value(value), Duration.class);
  }

  private Period period(String value) {
    return (Period) BuiltinConverter.Period.convert(value(value), Period.class);
  }

  private Value value(String value) {
    var mock = Mockito.mock(Value.class);
    Mockito.when(mock.value()).thenReturn(value);
    return mock;
  }
}

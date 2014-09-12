package jooby.internal;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jooby.HttpHeader;

import com.google.common.collect.ImmutableList;

public class SetHeader extends GetHeader implements HttpHeader {

  private Consumer<Iterable<String>> setter;

  public SetHeader(final String name, final List<String> value,
      final Consumer<Iterable<String>> setter) {
    super(name, value);
    this.setter = setter;
  }

  @Override
  public HttpHeader setString(final String value) {
    setter.accept(ImmutableList.of(value));
    return this;
  }

  @Override
  public HttpHeader setString(final Iterable<String> values) {
    setter.accept(values);
    return null;
  }

  @Override
  public HttpHeader setLong(final long value) {
    DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    Instant instant = Instant.ofEpochMilli(value);
    OffsetDateTime utc = instant.atOffset(ZoneOffset.UTC);
    return setString(formatter.format(utc));
  }

  @Override
  public HttpHeader setLong(final Iterable<Long> values) {
    DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    List<String> dates = new ArrayList<>();
    for (Long value : values) {
      Instant instant = Instant.ofEpochMilli(value);
      dates.add(formatter.format(instant));
    }
    return setString(dates);
  }

}

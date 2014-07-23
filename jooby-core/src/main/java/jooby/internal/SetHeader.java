package jooby.internal;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jooby.HttpMutableField;

import com.google.common.collect.ListMultimap;

public class SetHeader extends GetHeader implements HttpMutableField {

  private ListMultimap<String, String> headers;

  public SetHeader(final String name, final ListMultimap<String, String> headers) {
    super(name, headers.get(name));
    this.headers = headers;
  }

  @Override
  public HttpMutableField setString(final String value) {
    headers.removeAll(name);
    headers.put(name, value);
    return this;
  }

  @Override
  public HttpMutableField setString(final Iterable<String> values) {
    headers.removeAll(name);
    headers.putAll(name, values);
    return null;
  }

  @Override
  public HttpMutableField setLong(final long value) {
    DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    Instant instant = Instant.ofEpochMilli(value);
    OffsetDateTime utc = instant.atOffset(ZoneOffset.UTC);
    return setString(formatter.format(utc));
  }

  @Override
  public HttpMutableField setLong(final Iterable<Long> values) {
    DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    List<String> dates = new ArrayList<>();
    for (Long value : values) {
      Instant instant = Instant.ofEpochMilli(value);
      dates.add(formatter.format(instant));
    }
    return setString(dates);
  }

}

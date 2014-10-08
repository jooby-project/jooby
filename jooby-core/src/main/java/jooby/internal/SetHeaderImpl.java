package jooby.internal;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.function.BiConsumer;

import jooby.SetHeader;

public class SetHeaderImpl implements SetHeader {

  private BiConsumer<String, String> header;

  public SetHeaderImpl(final BiConsumer<String, String> header) {
    this.header = requireNonNull(header, "A header fn is required.");
  }

  @Override
  public SetHeader header(final String name, final byte value) {
    requireNonNull(name, "A header's name is required.");

    header.accept(name, Byte.toString(value));
    return this;
  }

  @Override
  public SetHeader header(final String name, final char value) {
    requireNonNull(name, "A header's name is required.");

    header.accept(name, Character.toString(value));
    return this;
  }

  @Override
  public SetHeader header(final String name, final double value) {
    requireNonNull(name, "A header's name is required.");

    // TODO: Decimal Formatter?
    header.accept(name, Double.toString(value));
    return this;
  }

  @Override
  public SetHeader header(final String name, final float value) {
    requireNonNull(name, "A header's name is required.");

    // TODO: Decimal Formatter?
    header.accept(name, Float.toString(value));
    return this;
  }

  @Override
  public SetHeader header(final String name, final int value) {
    requireNonNull(name, "A header's name is required.");

    header.accept(name, Integer.toString(value));
    return this;
  }

  @Override
  public SetHeader header(final String name, final long value) {
    requireNonNull(name, "A header's name is required.");

    header.accept(name, Long.toString(value));
    return this;
  }

  @Override
  public SetHeader header(final String name, final Date value) {
    requireNonNull(name, "A header's name is required.");
    requireNonNull(value, "A date value is required.");

    DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    Instant instant = Instant.ofEpochMilli(value.getTime());
    OffsetDateTime utc = instant.atOffset(ZoneOffset.UTC);

    header.accept(name, formatter.format(utc));
    return this;
  }

  @Override
  public SetHeader header(final String name, final short value) {
    requireNonNull(name, "A header's name is required.");

    header.accept(name, Short.toString(value));
    return this;
  }

  @Override
  public SetHeader header(final String name, final CharSequence value) {
    requireNonNull(name, "A header's name is required.");
    requireNonNull(value, "A header's value is required.");

    header.accept(name, value.toString());

    return this;
  }

}

package jooby;

import java.util.Date;

import javax.annotation.Nonnull;

public interface SetHeader {

  SetHeader header(@Nonnull String name, char value);

  SetHeader header(@Nonnull String name, byte value);

  SetHeader header(@Nonnull String name, short value);

  SetHeader header(@Nonnull String name, int value);

  SetHeader header(@Nonnull String name, long value);

  SetHeader header(@Nonnull String name, float value);

  SetHeader header(@Nonnull String name, double value);

  SetHeader header(@Nonnull String name, CharSequence value);

  SetHeader header(@Nonnull String name, Date value);

}

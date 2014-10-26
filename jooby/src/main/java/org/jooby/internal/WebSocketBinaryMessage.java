package org.jooby.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Variant;

import com.google.common.base.Charsets;
import com.google.inject.TypeLiteral;

public class WebSocketBinaryMessage implements Variant {

  private ByteBuffer buffer;

  public WebSocketBinaryMessage(final ByteBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public boolean isPresent() {
    return true;
  }

  @Override
  public boolean booleanValue() {
    throw typeError(boolean.class);
  }

  @Override
  public byte byteValue() {
    throw typeError(byte.class);
  }

  @Override
  public short shortValue() {
    throw typeError(short.class);
  }

  @Override
  public int intValue() {
    throw typeError(int.class);
  }

  @Override
  public long longValue() {
    throw typeError(long.class);
  }

  @Override
  public String stringValue() {
    throw typeError(String.class);
  }

  @Override
  public float floatValue() {
    throw typeError(float.class);
  }

  @Override
  public double doubleValue() {
    throw typeError(double.class);
  }

  @Override
  public <T extends Enum<T>> T enumValue(final Class<T> type) {
    throw typeError(type);
  }

  @Override
  public <T> List<T> toList(final Class<T> type) {
    throw typeError(type);
  }

  @Override
  public <T> Set<T> toSet(final Class<T> type) {
    throw typeError(type);
  }

  @Override
  public <T extends Comparable<T>> SortedSet<T> toSortedSet(final Class<T> type) {
    throw typeError(type);
  }

  @Override
  public <T> Optional<T> toOptional(final Class<T> type) {
    throw typeError(type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T to(final TypeLiteral<T> type) {
    Class<? super T> rawType = type.getRawType();
    if (rawType == byte[].class) {
      return (T) buffer.array();
    }
    if (rawType == ByteBuffer.class) {
      return (T) buffer;
    }
    if (rawType == InputStream.class) {
      return (T) new ByteArrayInputStream(buffer.array());
    }
    if (rawType == Reader.class) {
      return (T) new InputStreamReader(new ByteArrayInputStream(buffer.array()), Charsets.UTF_8);
    }
    throw typeError(rawType);
  }

  private Route.Err typeError(final Class<?> type) {
    return new Route.Err(Response.Status.BAD_REQUEST, "Can't convert to "
        + ByteBuffer.class.getName() + " to " + type);
  }
}

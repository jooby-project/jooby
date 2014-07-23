package jooby.internal;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import jooby.HttpException;
import jooby.HttpField;
import jooby.HttpStatus;
import jooby.Upload;

import com.google.common.collect.ImmutableSet;
import com.google.inject.TypeLiteral;

public class GetUpload implements HttpField {

  private String name;

  private List<Upload> uploads;

  public GetUpload(final String name, final List<Upload> uploads) {
    this.name = name;
    this.uploads = uploads;
  }

  @Override
  public boolean getBoolean() {
    throw typeError(boolean.class);
  }

  @Override
  public byte getByte() {
    throw typeError(byte.class);
  }

  @Override
  public short getShort() {
    throw typeError(short.class);
  }

  @Override
  public int getInt() {
    throw typeError(int.class);
  }

  @Override
  public long getLong() {
    throw typeError(long.class);
  }

  @Override
  public String getString() {
    throw typeError(String.class);
  }

  @Override
  public float getFloat() {
    throw typeError(float.class);
  }

  @Override
  public double getDouble() {
    throw typeError(double.class);
  }

  @Override
  public <T extends Enum<T>> T getEnum(final Class<T> type) {
    throw typeError(type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> getList(final Class<T> type) {
    if (type == Upload.class) {
      return (List<T>) uploads;
    }
    throw typeError(type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Set<T> getSet(final Class<T> type) {
    if (type == Upload.class) {
      return (Set<T>) ImmutableSet.copyOf(uploads);
    }
    throw typeError(type);
  }

  @Override
  public <T extends Comparable<T>> SortedSet<T> getSortedSet(final Class<T> type) {
    throw typeError(type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> getOptional(final Class<T> type) throws Exception {
    if (type == Upload.class) {
      return (Optional<T>) Optional.of(uploads.get(0));
    }
    if (type == File.class) {
      return (Optional<T>) Optional.of(uploads.get(0).file());
    }
    throw typeError(type);
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Override
  public <T> T get(final TypeLiteral<T> type) throws Exception {
    Class<? super T> rawType = type.getRawType();
    if (rawType == Upload.class) {
      return (T) uploads.get(0);
    }
    if (rawType == Optional.class) {
      return (T) getOptional(classFrom(type));
    }
    if (rawType == List.class) {
      return (T) getList(classFrom(type));
    }
    if (rawType == Set.class) {
      return (T) getSet(classFrom(type));
    }
    if (rawType == SortedSet.class) {
      return (T) getSortedSet((Class) classFrom(type));
    }
    throw typeError(rawType);
  }

  private static Class<?> classFrom(final TypeLiteral<?> type) {
    return classFrom(type.getType());
  }

  private static Class<?> classFrom(final Type type) {
    if (type instanceof Class) {
      return (Class<?>) type;
    }
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      Type actualType = parameterizedType.getActualTypeArguments()[0];
      return classFrom(actualType);
    }
    throw new HttpException(HttpStatus.BAD_REQUEST, "Unknown type: " + type);
  }

  private HttpException typeError(final Class<?> type) {
    return new HttpException(HttpStatus.BAD_REQUEST, "Can't convert to " + name + " to " + type);
  }
}

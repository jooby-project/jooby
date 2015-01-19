package org.jooby.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;

import org.jooby.Mutant;
import org.jooby.internal.reqparam.RootParamConverter;

import com.google.common.primitives.Primitives;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;

public class MutantImpl implements Mutant {

  private RootParamConverter converter;

  private Object[] values;

  public MutantImpl(final RootParamConverter converter, final Object[] values) {
    this.converter = converter;
    this.values = values;
  }

  public MutantImpl(final RootParamConverter converter, final List<? extends Object> headers) {
    this(converter, headers == null || headers.size() == 0
        ? null
        : headers.toArray(new Object[headers.size()]));
  }

  @Override
  public boolean booleanValue() {
    return to(TypeLiteral.get(boolean.class));
  }

  @Override
  public byte byteValue() {
    return to(TypeLiteral.get(byte.class));
  }

  @Override
  public short shortValue() {
    return to(TypeLiteral.get(short.class));
  }

  @Override
  public int intValue() {
    return to(TypeLiteral.get(int.class));
  }

  @Override
  public long longValue() {
    return to(TypeLiteral.get(long.class));
  }

  @Override
  public String stringValue() {
    return to(TypeLiteral.get(String.class));
  }

  @Override
  public float floatValue() {
    return to(TypeLiteral.get(float.class));
  }

  @Override
  public double doubleValue() {
    return to(TypeLiteral.get(double.class));
  }

  @Override
  public <T extends Enum<T>> T enumValue(final Class<T> type) {
    return to(TypeLiteral.get(type));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> toList(final Class<T> type) {
    return (List<T>) to(TypeLiteral.get(Types.listOf(Primitives.wrap(type))));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Set<T> toSet(final Class<T> type) {
    return (Set<T>) to(TypeLiteral.get(Types.setOf(Primitives.wrap(type))));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Comparable<T>> SortedSet<T> toSortedSet(final Class<T> type) {
    return (SortedSet<T>) to(TypeLiteral.get(
        Types.newParameterizedType(SortedSet.class, Primitives.wrap(type))
        ));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> toOptional(final Class<T> type) {
    return (Optional<T>) to(TypeLiteral.get(
        Types.newParameterizedType(Optional.class, Primitives.wrap(type))
        ));
  }

  @Override
  public <T> T to(final TypeLiteral<T> type) {
    return converter.convert(type, values);
  }

  @Override
  public boolean isPresent() {
    return values != null && values.length > 0;
  }

  @Override
  public String toString() {
    if (values == null || values.length == 0) {
      return "";
    }
    if (values.length == 1) {
      return values[0].toString();
    }
    return Arrays.toString(values);
  }

}

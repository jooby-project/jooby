package org.jooby.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jooby.Mutant;
import org.jooby.internal.reqparam.RootParamConverter;

import com.google.inject.TypeLiteral;

/**
 * Default mutant implementation.
 *
 * NOTE: This class isn't thread-safe.
 *
 * @author edgar
 */
public class MutantImpl implements Mutant {

  private final Map<Object, Object> results = new HashMap<>(1);

  private final RootParamConverter converter;

  private final Object[] values;

  public MutantImpl(final RootParamConverter converter, final Object[] values) {
    this.converter = converter;
    this.values = values;
  }

  public MutantImpl(final RootParamConverter converter,
      final List<? extends Object> headers) {
    this(converter, headers == null || headers.size() == 0
        ? null
        : headers.toArray(new Object[headers.size()]));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T to(final TypeLiteral<T> type) {
    T result = (T) results.get(type);
    if (result == null) {
      result = converter.convert(type, values);
      results.put(type, result);
    }
    return result;
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

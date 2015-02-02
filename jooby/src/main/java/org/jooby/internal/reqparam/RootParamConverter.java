package org.jooby.internal.reqparam;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.jooby.Err;
import org.jooby.ParamConverter;
import org.jooby.Status;

import com.google.common.collect.ImmutableList;
import com.google.inject.TypeLiteral;

public class RootParamConverter {

  private List<ParamConverter> chain;

  @Inject
  public RootParamConverter(final Set<ParamConverter> converters) {
    this.chain = ImmutableList.<ParamConverter> builder()
        .addAll(converters)
        .add((toType, values, chain) -> {
          throw new Err(Status.BAD_REQUEST, "No converter for " + toType);
        })
        .build();
  }

  public <T> T convert(final TypeLiteral<?> type, final Object value) {
    return convert(type, new Object[]{value });
  }

  @SuppressWarnings("unchecked")
  public <T> T convert(final TypeLiteral<?> type, final Object[] values) {
    try {
      requireNonNull(type, "A type is required.");
      return (T) chain(type, chain).convert(type, values);
    } catch (Err err) {
      throw err;
    } catch (Exception ex) {
      throw new Err(Status.BAD_REQUEST, ex);
    }
  }

  private static ParamConverter.Chain chain(final TypeLiteral<?> seed,
      final List<ParamConverter> converters) {
    return new ParamConverter.Chain() {
      int cursor = 0;

      TypeLiteral<?> type = seed;

      @Override
      public Object convert(final TypeLiteral<?> nextType, final Object[] nextVals)
          throws Exception {
        if (!type.equals(nextType)) {
          // reset cursor on type changes.
          cursor = 0;
          type = nextType;
        }
        ParamConverter next = converters.get(cursor);
        cursor += 1;
        Object result = next.convert(nextType, nextVals, this);
        cursor -= 1;
        return result;
      }
    };
  }
}

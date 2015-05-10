package org.jooby.internal.reqparam;

import java.util.List;
import java.util.Map;

import org.jooby.Mutant;
import org.jooby.Parser;
import org.jooby.Parser.Builder;
import org.jooby.Parser.Callback;
import org.jooby.Upload;

import com.google.common.collect.ImmutableMap;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;

@SuppressWarnings("rawtypes")
public class ParserBuilder implements Parser.Builder {

  private ImmutableMap.Builder<TypeLiteral<?>, Parser.Callback> strategies = ImmutableMap
      .builder();

  public final TypeLiteral<?> toType;

  private final TypeLiteral<?> type;

  public final Object value;

  private Parser.Context ctx;

  public ParserBuilder(final Parser.Context ctx, final TypeLiteral<?> toType, final Object value) {
    this.ctx = ctx;
    this.toType = toType;
    this.type = typeOf(value);
    this.value = value;
  }

  private TypeLiteral<?> typeOf(final Object value) {
    if (value instanceof List) {
      List values = (List) value;
      if (values.size() > 0) {
        if (values.iterator().next() instanceof Upload) {
          return TypeLiteral.get(Types.listOf(Upload.class));
        }
      }
      return TypeLiteral.get(Types.listOf(String.class));
    } else if (value instanceof Map) {
      return TypeLiteral.get(Types.mapOf(String.class, Mutant.class));
    } else if (value instanceof Parser.BodyReference) {
      return TypeLiteral.get(Parser.BodyReference.class);
    }
    return TypeLiteral.get(value.getClass());
  }

  @Override
  public Builder body(final Callback<Parser.BodyReference> callback) {
    strategies.put(TypeLiteral.get(Parser.BodyReference.class), callback);
    return this;
  }

  @Override
  public Builder param(final Callback<List<String>> callback) {
    strategies.put(TypeLiteral.get(Types.listOf(String.class)), callback);
    return this;
  }

  @Override
  public Builder params(final Callback<Map<String, Mutant>> callback) {
    strategies.put(TypeLiteral.get(Types.mapOf(String.class, Mutant.class)), callback);
    return this;
  }

  @Override
  public Builder upload(final Callback<List<Upload>> callback) {
    strategies.put(TypeLiteral.get(Types.listOf(Upload.class)), callback);
    return this;
  }

  @SuppressWarnings("unchecked")
  public Object parse() throws Exception {
    Map<TypeLiteral<?>, Callback> map = strategies.build();
    Callback callback = map.get(type);
    if (callback == null) {
      return ctx.next(toType, value);
    }
    return callback.invoke(value);
  }

}

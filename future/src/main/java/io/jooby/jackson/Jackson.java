package io.jooby.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.Context;
import io.jooby.Converter;
import io.jooby.Reified;

import javax.annotation.Nonnull;

public class Jackson extends Converter {

  private final ObjectMapper mapper;

  public Jackson(ObjectMapper mapper) {
    super("application/json");
    this.mapper = mapper;
  }

  public Jackson() {
    this(new ObjectMapper());
  }

  @Override public void render(@Nonnull Context ctx, @Nonnull Object value) throws Exception {
    if (value instanceof CharSequence) {
      // Ignore string/charsequence responses, those are going to be processed by the default renderer and let route to return raw JSON
      return;
    }
    ctx.send(mapper.writeValueAsBytes(value));
  }

  @Override public <T> T parse(Context ctx, Reified<T> type) throws Exception {
    JavaType javaType = mapper.getTypeFactory().constructType(type.getType());
    return mapper.readValue(ctx.body().stream(), javaType);
  }
}

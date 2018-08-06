package io.jooby.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.Context;
import io.jooby.Renderer;

import javax.annotation.Nonnull;

public class Jackson implements Renderer {

  private final ObjectMapper mapper;

  public Jackson(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public Jackson() {
    this(new ObjectMapper());
  }

  @Override public void render(@Nonnull Context ctx, @Nonnull Object value) throws Exception {
    ctx.send(mapper.writeValueAsBytes(value));
  }
}

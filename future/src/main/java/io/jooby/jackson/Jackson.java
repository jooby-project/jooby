package io.jooby.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.Handler;
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

  @Nonnull @Override public Handler apply(@Nonnull Handler next) throws Exception {
    return ctx -> {
      Object value = next.apply(ctx);
      if (!ctx.isResponseStarted()) {
        ctx.send(mapper.writeValueAsBytes(value));
      }
      return value;
    };
  }
}

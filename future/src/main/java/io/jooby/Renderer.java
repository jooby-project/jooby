package io.jooby;

public interface Renderer extends Filter {

  Renderer TO_STRING = next -> ctx -> {
    Object value = next.apply(ctx);
    if (!ctx.isResponseStarted())
      ctx.send(value.toString());
    return value;
  };
}

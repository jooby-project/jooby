package io.jooby;

import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseContext implements Context {

  protected final Map<String, Parser> parsers = new HashMap<>();

  protected final Route route;

  protected final Map<String, Object> locals = new HashMap<>();

  public BaseContext(@Nonnull Route route) {
    this.route = route;
  }

  @Nonnull @Override public Route route() {
    return route;
  }

  @Nonnull @Override public Map<String, Object> locals() {
    return locals;
  }

  @Nonnull @Override public Parser parser(@Nonnull String contentType) {
    return parsers.getOrDefault(contentType, Parser.NOT_ACCEPTABLE);
  }

  @Nonnull @Override public Context parser(@Nonnull String contentType, @Nonnull Parser parser) {
    parsers.put(contentType, parser);
    return this;
  }

  @Nonnull @Override public Context render(@Nonnull Object result) {
    try {
      route.renderer().render(this, result);
      return this;
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  protected void requireBlocking() {
    if (isInIoThread()) {
      throw new IllegalStateException(
          "Attempted to do blocking IO from the IO thread. This is prohibited as it may result in deadlocks");
    }
  }
}

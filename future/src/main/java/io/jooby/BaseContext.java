package io.jooby;

import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseContext implements Context {

  protected final Map<String, Parser> parsers = new HashMap<>();

  protected Route route;

  protected final Map<String, Object> locals = new HashMap<>();

  private Route.After after;

  private Map<String, String> params;

  public BaseContext() {
  }

  @Nonnull @Override public Route route() {
    return route;
  }

  public void prepare(Router.Match match) {
    this.route = match.route();
    this.after = route.after();
    this.params = match.params();
  }

  @Nonnull @Override public Map<String, String> params() {
    return params;
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

  @Nonnull @Override public Context send(@Nonnull Object result) {
    try {
      route.renderer().render(this, fireAfter(result));
      return this;
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  protected Object fireAfter(Object result) {
    if (this.after != null) {
      Route.After chain = this.after;
      this.after = null;
      try {
        return chain.apply(this, result);
      } catch (Exception x) {
        throw Throwing.sneakyThrow(x);
      }
    }
    return result;
  }

  protected void requireBlocking() {
    if (isInIoThread()) {
      throw new IllegalStateException(
          "Attempted to do blocking IO from the IO thread. This is prohibited as it may result in deadlocks");
    }
  }
}

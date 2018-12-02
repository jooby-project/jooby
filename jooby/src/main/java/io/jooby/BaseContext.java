package io.jooby;

import org.jooby.funzy.Throwing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseContext implements Context {

  protected Map<String, Parser> parsers = Collections.EMPTY_MAP;

  protected Route route;

  protected Map<String, Object> locals = Collections.EMPTY_MAP;

  private Map<String, String> params;

  public BaseContext() {
  }

  @Nonnull @Override public Route route() {
    return route;
  }

  public void prepare(Router.Match match) {
    this.route = match.route();
    this.params = match.params();
  }

  @Nonnull @Override public Map<String, String> params() {
    return params;
  }

  @Nonnull @Override public Map<String, Object> locals() {
    return locals;
  }

  @Nullable @Override public <T> T get(String name) {
    return (T) locals.get(name);
  }

  @Nonnull @Override public Context set(@Nonnull String name, @Nonnull Object value) {
    if (locals == Collections.EMPTY_MAP) {
      locals = new HashMap<>();
    }
    locals.put(name, value);
    return this;
  }

  @Nonnull @Override public Parser parser(@Nonnull String contentType) {
    return parsers.getOrDefault(contentType, Parser.NOT_ACCEPTABLE);
  }

  @Nonnull @Override public Context parser(@Nonnull String contentType, @Nonnull Parser parser) {
    if (parsers == Collections.EMPTY_MAP) {
      parsers = new HashMap<>();
    }
    parsers.put(contentType, parser);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull Object result) {
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
          "Attempted to do blocking EVENT_LOOP from the EVENT_LOOP thread. This is prohibited as it may result in deadlocks");
    }
  }
}

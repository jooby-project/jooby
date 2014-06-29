package jooby;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Switch<In, Out> {

  public interface Fn<T> {
    T apply() throws Exception;
  }

  private final Map<In, Fn<Out>> strategies = new HashMap<>();

  private In value;

  public Switch(final In value) {
    this.value = requireNonNull(value, "The value is required.");
  }

  public Switch<In, Out> when(final In value, final Out result) {
    return when(value, () -> result);
  }

  public Switch<In, Out> when(final In value, final Fn<Out> fn) {
    requireNonNull(value, "A value is required.");
    requireNonNull(fn, "A function is required.");
    strategies.put(value, fn);
    return this;
  }

  private Fn<Out> fn(final In value) {
    Fn<Out> fn = strategies.get(value);
    return fn == null ? strategies.get("*") : fn;
  }

  public Out get() throws Exception {
    return Optional.ofNullable(fn(value))
        .orElseThrow(() -> new IllegalStateException("No switch for: '" + value + "'"))
        .apply();
  }

  public Out get(final Out otherwise) throws Exception {
    Fn<Out> fn = fn(value);
    return fn == null ? otherwise : fn.apply();
  }
}

package jooby;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Switch<T> {

  public interface Fn<T> {
    T apply() throws Exception;
  }

  public interface Always<T> {
    T apply(T value) throws Exception;
  }

  private final Map<String, Fn<T>> strategies = new HashMap<>();

  private String value;

  public Switch(final String value) {
    this.value = requireNonNull(value, "The value is required.");
  }

  public Switch<T> on(final String value, final Fn<T> fn) {
    requireNonNull(value, "A value is required.");
    requireNonNull(fn, "A function is required.");
    strategies.put(value.toLowerCase(), fn);
    return this;
  }

  private Fn<T> fn(final String value) {
    Fn<T> fn = strategies.get(value);
    return fn == null ? strategies.get("*") : fn;
  }

  public T execute() throws Exception {
    return Optional.ofNullable(fn(value))
        .orElseThrow(() -> new IllegalStateException("No switch for: '" + value + "'"))
        .apply();
  }

  public T execute(final T defaultValue) throws Exception {
    Fn<T> fn = fn(value);
    return fn == null ? defaultValue : fn.apply();
  }
}

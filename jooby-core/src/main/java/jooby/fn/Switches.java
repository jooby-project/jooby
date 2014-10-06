package jooby.fn;

import static java.util.Objects.requireNonNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;

public class Switches {

  private static class DefSwitch<In, Out> implements Switch<In, Out> {

    private final Map<Predicate<In>, ExSupplier<Out>> strategies = new LinkedHashMap<>();

    private In value;

    public DefSwitch(final In value) {
      this.value = requireNonNull(value, "The value is required.");
    }

    @Override
    public Switch<In, Out> when(final In value, final Out result) {
      return when(value, () -> result);
    }

    @Override
    public Switch<In, Out> when(final Predicate<In> predicate, final Out result) {
      return when(predicate, () -> result);
    }

    @Override
    public Switch<In, Out> when(final In value, final ExSupplier<Out> fn) {
      return when(value::equals, fn);
    }

    @Override
    public Switch<In, Out> when(final Predicate<In> predicate, final ExSupplier<Out> fn) {
      requireNonNull(value, "A value is required.");
      requireNonNull(fn, "A function is required.");
      strategies.put(predicate, fn);
      return this;
    }

    @Override
    public Out get() throws Exception {
      Out out = otherwise(null);
      if (out == null) {
        throw new NoSuchElementException("No value present");
    }
      return out;
    }

    @Override
    public Out otherwise(final Out otherwise) throws Exception {
      Set<Entry<Predicate<In>, ExSupplier<Out>>> entrySet = strategies.entrySet();
      for (Entry<Predicate<In>, ExSupplier<Out>> entry : entrySet) {
        if (entry.getKey().test(value)) {
          return entry.getValue().get();
        }
      }
      return otherwise;
    }

  }

  public static <In, Out> Switch<In, Out> newSwitch(final In value) {
    return new DefSwitch<In, Out>(value);
  }

  public static <Out> Switch<String, Out> newSwitch(final String value) {
    return new DefSwitch<String, Out>(value);
  }

}

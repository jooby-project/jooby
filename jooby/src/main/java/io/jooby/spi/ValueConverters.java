package io.jooby.spi;

import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import io.jooby.TypeMismatchException;

/**
 * Contains the {@link ValueConverter}s loaded via the ServiceLoader. It is is a
 * singleton and an instance can be retrieved with {@link #getInstance()}. The
 * ValueConverters are stored in an ordered collection and thus resolution of
 * type to be converted is based on the order of the converters.
 *
 * @author agentgt
 *
 */
public final class ValueConverters {

  // Allow thread safe adding of ValueConverters.
  private final CopyOnWriteArrayList<ValueConverter> valueConverters;

  // Initialization on demand
  private static final class Hidden {

    private static final ValueConverters INSTANCE = ValueConverters.create().fromServiceLoader();
  }

  private ValueConverters(CopyOnWriteArrayList<ValueConverter> valueConverters) {
    super();
    this.valueConverters = valueConverters;
  }

  static ValueConverters create() {
    return new ValueConverters(new CopyOnWriteArrayList<>());
  }

  /**
   * Attempts to convert values to an object based on the provided type.
   *
   * @param v
   *          value
   * @param c
   *          desired type
   * @return the type if converted or null if conversion was not possible.
   * @throws TypeMismatchException failure in a converter
   */
  public @Nullable Object convert(ValueContainer v, Class<?> c) throws TypeMismatchException {
    Object result = null;
    for (ValueConverter vc : valueConverters) {
      if (vc.supportsType(c)) {
        result = vc.convert(v, c);
        if (result != null) {
          return result;
        }
      }
    }
    return result;
  }

  ValueConverters fromServiceLoader() {
    ServiceLoader<ValueConverter> sl = ServiceLoader.load(ValueConverter.class);
    // If any failes to load we will fail entirely.
    // The value converters found earlier in the classpath take precedence.
    sl.forEach(this::add);
    return this;
  }

  /**
   * You can add value converters programmatic. For now its protected. Its also
   * to aid unit testing since serviceloader is inherently static singleton.
   *
   * @param vc
   * @return
   */
  /* private */ ValueConverters add(ValueConverter vc) {
    valueConverters.add(vc);
    return this;
  }

  ValueConverters clear() {
    valueConverters.clear();
    return this;
  }

  /**
   * The ValueConverters singleton usually preloaded by the ServiceLoader.
   * @return the shared singleton used by Jooby
   */
  public static ValueConverters getInstance() {
    return ValueConverters.Hidden.INSTANCE;
  }

}

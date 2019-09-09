/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import javax.annotation.Nullable;

import io.jooby.TypeMismatchException;
import io.jooby.Value;

/**
 * Contains the {@link BeanValueConverter}s loaded via the ServiceLoader. It is is a
 * singleton and an instance can be retrieved with {@link #getInstance()}. The
 * ValueConverters are stored in an ordered collection and thus resolution of
 * type to be converted is based on the order of the converters.
 *
 * @author agentgt
 *
 */
public final class BeanValueConverters {

  // Allow thread safe adding of ValueConverters.
  private final Iterable<BeanValueConverter> valueConverters;

  // Initialization on demand
  private static final class Hidden {

    private static volatile BeanValueConverters instance = BeanValueConverters.builder().fromServiceLoader().build();
  }

  private BeanValueConverters(Iterable<BeanValueConverter> valueConverters) {
    super();
    this.valueConverters = valueConverters;
  }

  static Builder builder() {
    return new Builder();
  }

  static final class Builder {

    private final List<BeanValueConverter> valueConverters = new ArrayList<>();

    Builder fromServiceLoader() {
      ServiceLoader<BeanValueConverter> sl = ServiceLoader.load(BeanValueConverter.class);
      // If any failes to load we will fail entirely.
      // The value converters found earlier in the classpath take precedence.
      sl.forEach(this::add);
      return this;
    }

    /**
     * You can add value converters programmatic. For now its protected. Its
     * also to aid unit testing since serviceloader is inherently static
     * singleton.
     *
     * @param vc
     * @return
     */
    Builder add(BeanValueConverter vc) {
      valueConverters.add(vc);
      return this;
    }

    Builder clear() {
      valueConverters.clear();
      return this;
    }

    BeanValueConverters build() {
      return new BeanValueConverters(valueConverters);
    }

    BeanValueConverters set() {
      BeanValueConverters vc = build();
      Hidden.instance = vc;
      return vc;
    }
  }

  /**
   * Attempts to convert values to an object based on the provided type.
   *
   * @param v
   *          value
   * @param c
   *          desired type
   * @return the type if converted or null if conversion was not possible.
   * @throws TypeMismatchException
   *           failure in a converter
   */
  public @Nullable Object convert(Value v, Class<?> c) throws TypeMismatchException {
    Object result = null;
    for (BeanValueConverter vc : valueConverters) {
      if (vc.supportsType(c)) {
        result = vc.convert(v, c);
        if (result != null) {
          return result;
        }
      }
    }
    return result;
  }

  /**
   * The ValueConverters singleton usually preloaded by the ServiceLoader.
   *
   * @return the shared singleton used by Jooby
   */
  public static BeanValueConverters getInstance() {
    return BeanValueConverters.Hidden.instance;
  }

}

package io.jooby.spi;

import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import io.jooby.TypeMismatchException;

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
  
  final ValueConverters fromServiceLoader() {
    ServiceLoader<ValueConverter> sl = ServiceLoader.load(ValueConverter.class);
    //If any failes to load we will fail entirely.
    //The value converters found earlier in the classpath take precedence.
    sl.forEach(this::add);
    return this;
  }
  /**
   * You can add value converters programmatic. For now its protected.
   * Its also to aid unit testing since serviceloader is inherently static singleton.
   * @param vc
   * @return
   */
  /* private */ final ValueConverters add(ValueConverter vc) {
    valueConverters.add(vc);
    return this;
  }
  
  final ValueConverters clear() {
    valueConverters.clear();
    return this;
  }
  
  
  
  public static final ValueConverters getInstance() {
    return ValueConverters.Hidden.INSTANCE;
  }

}

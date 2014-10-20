package org.jooby.fn;

public interface ExSupplier<T> {
  T get() throws Exception;
}

package jooby;

public interface ThrowingSupplier<T> {
  T get() throws Exception;
}

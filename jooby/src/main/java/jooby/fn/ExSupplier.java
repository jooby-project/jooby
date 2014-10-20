package jooby.fn;

public interface ExSupplier<T> {
  T get() throws Exception;
}

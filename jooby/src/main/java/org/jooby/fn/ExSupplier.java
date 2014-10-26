package org.jooby.fn;

/**
 * Represents a supplier of results.
 *
 * <p>
 * There is no requirement that a new or distinct result be returned each time the supplier is
 * invoked.
 *
 * @author edgar
 * @param <T> the type of results supplied by this supplier
 */
public interface ExSupplier<T> {

  /**
   * Get a result or throw an exception.
   *
   * @return A result.
   * @throws Exception If something goes wrong.
   */
  T get() throws Exception;
}

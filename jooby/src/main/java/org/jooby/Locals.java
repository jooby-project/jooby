package org.jooby;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Local attributes for {@link Request} and {@link Session}.
 *
 * @author edgar
 */
public interface Locals {

  /**
   * Get a named object. If the object isn't found this method returns an empty
   * optional.
   *
   * @param name A local var's name.
   * @param <T> Target type.
   * @return A value or empty optional.
   */
  @Nonnull
  <T> Optional<T> get(final @Nonnull String name);

  /**
   * @return An immutable copy of local attributes.
   */
  @Nonnull
  Map<String, Object> attributes();

  /**
   * Test if the var name exists inside the local attributes.
   *
   * @param name A local var's name.
   * @return True, for existing locals.
   */
  default boolean isSet(final @Nonnull String name) {
    return get(name).isPresent();
  }

  /**
   * Set a local using a the given name. If a local already exists, it will be replaced
   * with the new value. Keep in mind that ONLY none null values are allowed.
   *
   * @param name A local var's name.
   * @param value A local values.
   * @return This locals.
   */
  @Nonnull Locals set(final @Nonnull String name, final @Nonnull Object value);

  /**
   * Remove a local value (if any) from session locals.
   *
   * @param name A local var's name.
   * @param <T> A local type.
   * @return Existing value or empty optional.
   */
  <T> Optional<T> unset(final String name);

  /**
   * Unset/remove all the session data.
   *
   * @return This locals.
   */
  Locals unset();

}

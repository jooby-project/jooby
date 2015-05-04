package org.jooby.reflect;

import java.lang.reflect.Executable;

/**
 * Extract parameter names from a executable: method or constructor.
 *
 * @author edgar
 * @since 0.6.0
 */
public interface ParameterNameProvider {

  /**
   * Extract parameter names from a executable: method or constructor.
   *
   * @param exec Method or constructor.
   * @return Names or zero array length.
   */
  String[] names(Executable exec);
}

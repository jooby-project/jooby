package org.jooby;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Instances of {@link Managed} are started and/or stopped by Jooby.
 *
 * Start callback is executed on every single Guice provided object, regardless of the scope.
 *
 * Stop callback are ONLY supported for singleton objects.
 *
 * Beside the start callback will be invoked for every Guice object, it makes sense to only use
 * these calls on Singleton object who are expensive to create.
 *
 * This interface can be implemented by Provider too (not just final object).
 *
 * If you prefer, annotations just mark the start callback with {@link PostConstruct} and the stop
 * callback with {@link PreDestroy}.
 *
 * @author edgar
 * @since 0.5.0
 */
public interface Managed {

  /**
   * Start callback, useful to initialize an expensive service.
   *
   * @throws Exception If something goes wrong.
   */
  void start() throws Exception;

  /**
   * Stop callback, useful for cleanup and free resources. ONLY for singleton objects.
   *
   * @throws Exception If something goes wrong.
   */
  void stop() throws Exception;

}

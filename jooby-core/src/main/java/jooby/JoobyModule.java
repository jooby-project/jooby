package jooby;

import com.google.common.annotations.Beta;
import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * A module can publish or produces: {@link Route routes}, {@link BodyConverter converters},
 * {@link RequestModule request modules}, {@link RouteInterceptor interceptors} and any other
 * application specific service or contract of your choice.
 * <p>
 * It is similar to {@link com.google.inject.Module} except for the callback method that receive a
 * {@link Mode}, {@link Config} and {@link Binder}.
 * </p>
 *
 * <p>
 * A module can provide his own set of properties through the {@link #config()} method. By default,
 * This method returns an empty config object.
 * </p>
 *
 * <p>
 * A module can provide start/stop methods in order to start or close resources.
 * </p>
 *
 * @author edgar
 * @since 0.1.0
 * @see Jooby#use(JoobyModule)
 */
@Beta
public interface JoobyModule {

  /**
   * @return Produces a module config object (when need it). By default a module doesn't produce
   *         any configuration object.
   */
  default Config config() {
    return ConfigFactory.empty();
  }

  /**
   * Callback method to start a module.
   *
   * @throws Exception If something goes wrong.
   */
  default void start() throws Exception {
  }

  /**
   * Callback method to stop a module and clean any resources..
   *
   * @throws Exception If something goes wrong.
   */
  default void stop() throws Exception {
  }

  /**
   * Configure and produces bindings for the underlying application. A module can optimize or
   * customize a service by checking current the {@link Mode application mode} and/or the current
   * application properties available from {@link Config}.
   *
   * @param mode The current application's mode. Not null.
   * @param config The current config object. Not null.
   * @param binder A guice binder. Not null.
   * @throws Exception If the module fails during configuration.
   */
  void configure(Mode mode, Config config, Binder binder) throws Exception;
}

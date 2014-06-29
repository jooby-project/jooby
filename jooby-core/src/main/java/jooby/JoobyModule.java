package jooby;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public interface JoobyModule {

  default String name() {
    StringBuilder name = new StringBuilder(getClass().getSimpleName());
    name.setCharAt(0, Character.toLowerCase(name.charAt(0)));
    return name.toString();
  }

  default Config config() {
    return ConfigFactory.parseResources(name() + ".conf");
  }

  default void start() throws Exception {
  }

  default void stop() throws Exception {
  }

  void configure(Mode mode, Config config, Binder binder) throws Exception;
}

package org.jooby.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class JoobySuite extends Suite {

  private List<Runner> runners;

  static {
    System.setProperty("io.netty.leakDetectionLevel", "advanced");
  }

  public JoobySuite(final Class<?> klass) throws InitializationError {
    super(klass, Collections.emptyList());

    runners = runners(klass);
  }

  @SuppressWarnings("rawtypes")
  private List<Runner> runners(final Class<?> klass) throws InitializationError {
    List<Runner> runners = new ArrayList<>();
    Predicate<Class> filter = Predicates.alwaysTrue();
    OnServer onserver = klass.getAnnotation(OnServer.class);
    if (onserver != null) {
      List<Class<?>> server = Arrays.asList(onserver.value());
      filter = server::contains;
    }
    String[] servers = {"org.jooby.undertow.Undertow", "org.jooby.jetty.Jetty",
        "org.jooby.netty.Netty" };
    for (String server : servers) {
      try {
        Class<?> serverClass = getClass().getClassLoader().loadClass(server);
        if (filter.apply(serverClass)) {
          runners.add(new JoobyRunner(getTestClass().getJavaClass(), serverClass));
        }
      } catch (ClassNotFoundException ex) {
        // do nothing
      }
    }
    return runners;
  }

  @Override
  protected List<Runner> getChildren() {
    return runners;
  }
}

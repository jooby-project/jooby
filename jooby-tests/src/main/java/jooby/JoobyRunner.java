package jooby;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.multibindings.OptionalBinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class JoobyRunner extends BlockJUnit4ClassRunner {

  private Jooby app;
  private int port;

  public JoobyRunner(final Class<?> klass) throws InitializationError {
    super(klass);
    start(klass);
  }

  private void start(final Class<?> klass) throws InitializationError {
    try {
      App annotation = klass.getAnnotation(App.class);
      Class<?> appClass = klass;
      port = freePort();
      if (annotation != null) {
        if (annotation.value() != Jooby.class) {
          appClass = annotation.value();
        }
      }
      if (!Jooby.class.isAssignableFrom(appClass)) {
        throw new InitializationError("Invalid jooby app: " + appClass);
      }
      Config testConfig = ConfigFactory.empty()
          .withValue("jooby.internal.server.test", ConfigValueFactory.fromAnyRef(true))
          .withValue("application.port", ConfigValueFactory.fromAnyRef(port))
          .withValue("ssl.keystore.path", ConfigValueFactory.fromAnyRef("/missing/keystore"));

      app = (Jooby) appClass.newInstance();
      app.use(new Jooby.Module() {
        @Override
        public void configure(final Mode mode, final Config config, final Binder binder)
            throws Exception {
          OptionalBinder.newOptionalBinder(binder, Server.class).setBinding()
              .to(NoJoinServer.class)
              .in(Singleton.class);
        }

        @Override
        public Config config() {
          return testConfig;
        }
      });
      app.start();
    } catch (Exception ex) {
      throw new InitializationError(Arrays.asList(ex));
    }
  }

  @Override
  protected Object createTest() throws Exception {
    Object test = super.createTest();
    Guice.createInjector(new Module() {
      @Override
      public void configure(final Binder binder) {
        binder.bind(Integer.class).annotatedWith(Names.named("jooby.http.port")).toInstance(port);
        binder.bind(Integer.class).annotatedWith(Names.named("port")).toInstance(port);
      }
    }).injectMembers(test);
    ;
    return test;
  }

  @Override
  protected Statement withAfterClasses(final Statement statement) {
    Statement next = super.withAfterClasses(statement);
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        List<Throwable> errors = new ArrayList<Throwable>();
        try {
          next.evaluate();
        } catch (Throwable e) {
          errors.add(e);
        }

        try {
          app.stop();
        } catch (Exception e) {
          errors.add(e);
        }

        if (errors.isEmpty()) {
          return;
        }
        if (errors.size() == 1) {
          throw errors.get(0);
        }
        throw new MultipleFailureException(errors);
      }
    };
  }

  private int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}

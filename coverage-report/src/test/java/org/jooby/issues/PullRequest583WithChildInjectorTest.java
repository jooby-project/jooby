package org.jooby.issues;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.jooby.test.ServerFeature;
import org.junit.*;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Johannes Schneider (<a href="mailto:js@cedarsoft.com">js@cedarsoft.com</a>)
 */
public class PullRequest583WithChildInjectorTest extends ServerFeature {
  private final AtomicReference<Injector> createdInjector = new AtomicReference<>();

  private final Injector parentInjector = Guice.createInjector(
    binder -> binder.bind(MyInjectedClass.class).toInstance(new MyInjectedClass()));

  {

    injector((stage, module) -> {
      Injector injector = parentInjector.createChildInjector(module);
      createdInjector.set(injector);
      return injector;
    });
  }

  @Test
  public void appShouldBeMountedOnApplicationPath() throws Exception {
    Assert.assertNull(createdInjector.get());

    start();
    try {
      Injector injector = require(Injector.class);
      Assert.assertSame(parentInjector.getInstance(MyInjectedClass.class), injector.getInstance(MyInjectedClass.class));
    } finally {
      stop();
    }
  }

  private static class MyInjectedClass {
  }

}

package org.jooby.issues;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.jooby.test.ServerFeature;
import org.junit.*;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Johannes Schneider (<a href="mailto:js@cedarsoft.com">js@cedarsoft.com</a>)
 */
public class PullRequest583Test extends ServerFeature {
  private final AtomicReference<Injector> createdInjector = new AtomicReference<>();

  {
    injector((stage, module) -> {
      Injector injector = Guice.createInjector(module);
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
      Assert.assertNotNull(injector);
      Assert.assertSame(injector, createdInjector.get());
    } finally {
      stop();
    }
  }
}
